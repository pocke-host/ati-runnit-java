package com.runnit.api.security;

import com.runnit.api.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.ProtectedHeader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Validates Google ID tokens locally using JWKS public key verification.
 * Replaces the deprecated tokeninfo endpoint with cryptographic verification
 * per Google's production best practices:
 * https://developers.google.com/identity/protocols/oauth2/openid-connect#validatinganidtoken
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenValidator {

    private static final String JWKS_URL = "https://www.googleapis.com/oauth2/v3/certs";
    private static final long JWKS_CACHE_TTL_MS = 3_600_000L; // 1 hour
    private static final Set<String> VALID_ISSUERS = Set.of(
            "https://accounts.google.com",
            "accounts.google.com"
    );

    private final RestTemplate restTemplate;

    @Value("${google.client.id:}")
    private String googleClientId;

    private record JwksSnapshot(Map<String, PublicKey> keys, long cachedAt) {
        static final JwksSnapshot EMPTY = new JwksSnapshot(Map.of(), 0L);
    }

    // Atomic snapshot so the key map and its timestamp always swap together
    private final AtomicReference<JwksSnapshot> jwksSnapshot = new AtomicReference<>(JwksSnapshot.EMPTY);

    /**
     * Validates a Google ID token using JWKS cryptographic verification.
     * Checks: signature, issuer, audience, expiry, email_verified.
     *
     * @param idToken the Google ID token (JWT) from the client
     * @return verified GoogleClaims
     * @throws UnauthorizedException if validation fails for any reason
     */
    public GoogleClaims validate(String idToken) {
        try {
            Claims claims = Jwts.parser()
                    .keyLocator(header -> {
                        if (header instanceof ProtectedHeader ph) {
                            return getPublicKey(ph.getKeyId());
                        }
                        throw new UnauthorizedException("Token is not a signed JWT");
                    })
                    .build()
                    .parseSignedClaims(idToken)
                    .getPayload();

            // Validate issuer
            String iss = claims.getIssuer();
            if (!VALID_ISSUERS.contains(iss)) {
                log.warn("[google] Token issuer invalid: {}", iss);
                throw new UnauthorizedException("Invalid Google token issuer");
            }

            // Validate audience matches our OAuth client
            if (googleClientId != null && !googleClientId.isBlank()) {
                Set<String> aud = claims.getAudience();
                if (aud == null || !aud.contains(googleClientId)) {
                    log.warn("[google] Token audience mismatch: expected={} got={}", googleClientId, aud);
                    throw new UnauthorizedException("Google token audience mismatch");
                }
            }

            // Validate expiry (jjwt enforces this, but be explicit for logging)
            if (claims.getExpiration() == null || claims.getExpiration().toInstant().isBefore(Instant.now())) {
                throw new UnauthorizedException("Google ID token has expired");
            }

            // Require a verified email — prevents login via unverified Google accounts
            Boolean emailVerified = claims.get("email_verified", Boolean.class);
            if (!Boolean.TRUE.equals(emailVerified)) {
                throw new UnauthorizedException("Google account email is not verified");
            }

            return new GoogleClaims(
                    claims.getSubject(),
                    claims.get("email", String.class),
                    claims.get("name", String.class),
                    claims.get("picture", String.class)
            );

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[google] ID token validation failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Google ID token");
        }
    }

    private PublicKey getPublicKey(String kid) {
        refreshJwksIfStale();
        PublicKey key = jwksSnapshot.get().keys().get(kid);
        if (key == null) {
            // Key may have rotated — force a fresh fetch and retry once
            loadJwks();
            key = jwksSnapshot.get().keys().get(kid);
        }
        if (key == null) {
            throw new UnauthorizedException("Unknown Google signing key: " + kid);
        }
        return key;
    }

    private void refreshJwksIfStale() {
        JwksSnapshot snap = jwksSnapshot.get();
        long now = System.currentTimeMillis();
        if (snap.keys().isEmpty() || now - snap.cachedAt() > JWKS_CACHE_TTL_MS) {
            loadJwks();
        }
    }

    @SuppressWarnings("unchecked")
    private synchronized void loadJwks() {
        // Double-checked: another thread may have refreshed while we were waiting on the lock
        JwksSnapshot current = jwksSnapshot.get();
        if (!current.keys().isEmpty() && System.currentTimeMillis() - current.cachedAt() <= JWKS_CACHE_TTL_MS) {
            return;
        }
        try {
            Map<String, Object> response = restTemplate.getForObject(JWKS_URL, Map.class);
            if (response == null) throw new IllegalStateException("Empty JWKS response from Google");

            List<Map<String, String>> keys = (List<Map<String, String>>) response.get("keys");
            Map<String, PublicKey> fresh = new HashMap<>();
            for (Map<String, String> jwk : keys) {
                String kid = jwk.get("kid");
                String n   = jwk.get("n");
                String e   = jwk.get("e");
                if (kid != null && n != null && e != null) {
                    fresh.put(kid, buildRsaPublicKey(n, e));
                }
            }
            jwksSnapshot.set(new JwksSnapshot(Map.copyOf(fresh), System.currentTimeMillis()));
            log.info("[google] JWKS refreshed: {} keys cached", fresh.size());
        } catch (Exception ex) {
            log.error("[google] JWKS refresh failed: {}", ex.getMessage());
            // If we have stale keys, continue using them rather than hard-failing
            if (jwksSnapshot.get().keys().isEmpty()) {
                throw new UnauthorizedException("Google token validation unavailable — JWKS fetch failed");
            }
        }
    }

    private PublicKey buildRsaPublicKey(String nBase64Url, String eBase64Url) {
        try {
            BigInteger modulus  = new BigInteger(1, Base64.getUrlDecoder().decode(nBase64Url));
            BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(eBase64Url));
            return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(modulus, exponent));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to build RSA public key from JWKS", ex);
        }
    }

    public record GoogleClaims(String sub, String email, String name, String picture) {}
}
