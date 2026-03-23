package com.runnit.api.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Validates Apple Sign-In identity tokens.
 *
 * Flow:
 *  1. Parse the JWT header to get the key ID (kid).
 *  2. Fetch Apple's JWKS (cached for 1 hour) and find the matching RSA public key.
 *  3. Validate the JWT signature + claims (iss, aud, exp) using JJWT.
 *
 * No extra dependencies needed — uses Jackson (already in Spring Web) + JJWT.
 * Apple JWKS docs: https://developer.apple.com/documentation/sign_in_with_apple/fetch_apple_s_public_key_for_verifying_token_signature
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenValidator {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER   = "https://appleid.apple.com";
    private static final long   CACHE_TTL_MS   = 60 * 60 * 1000L; // 1 hour

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${apple.client.id:}")
    private String appleClientId;

    // Simple in-memory JWKS cache: kid -> PublicKey
    private final ConcurrentHashMap<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private volatile long cacheLoadedAt = 0;

    /**
     * Validates an Apple identity token and returns the verified claims.
     *
     * @param identityToken the identityToken string from Apple's auth response
     * @return AppleClaims with sub, email (may be null for private relay users)
     * @throws UnauthorizedException if the token is invalid
     */
    public AppleClaims validate(String identityToken) {
        String kid = extractKid(identityToken);
        PublicKey publicKey = getPublicKey(kid);

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(identityToken)
                    .getPayload();
        } catch (Exception e) {
            log.warn("Apple token validation failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Apple identity token");
        }

        // Verify issuer
        if (!APPLE_ISSUER.equals(claims.getIssuer())) {
            throw new UnauthorizedException("Apple token issuer mismatch");
        }

        // Verify audience matches our service ID / bundle ID
        if (appleClientId != null && !appleClientId.isBlank()) {
            String aud = claims.getAudience() != null ? claims.getAudience().iterator().next() : null;
            if (!appleClientId.equals(aud)) {
                log.warn("Apple token aud mismatch: expected={}, got={}", appleClientId, aud);
                throw new UnauthorizedException("Apple token audience mismatch");
            }
        }

        // Verify not expired (JJWT checks this automatically, but just in case)
        if (claims.getExpiration() != null && claims.getExpiration().toInstant().isBefore(Instant.now())) {
            throw new UnauthorizedException("Apple identity token has expired");
        }

        String sub   = claims.getSubject();                    // stable Apple user ID
        String email = claims.get("email", String.class);      // may be null (private relay) or relay address

        return new AppleClaims(sub, email);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private String extractKid(String token) {
        // JWT = header.payload.signature — header is base64url encoded JSON
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new UnauthorizedException("Malformed Apple identity token");
        }
        try {
            byte[] headerBytes = Base64.getUrlDecoder().decode(parts[0]);
            Map<String, Object> header = objectMapper.readValue(headerBytes, new TypeReference<>() {});
            String kid = (String) header.get("kid");
            if (kid == null) throw new UnauthorizedException("Apple token missing kid");
            return kid;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Could not parse Apple token header");
        }
    }

    private PublicKey getPublicKey(String kid) {
        long now = System.currentTimeMillis();
        if (now - cacheLoadedAt > CACHE_TTL_MS) {
            refreshKeyCache();
        }

        PublicKey key = keyCache.get(kid);
        if (key == null) {
            // Cache miss after fresh load — refresh once more in case Apple rotated keys
            refreshKeyCache();
            key = keyCache.get(kid);
        }
        if (key == null) {
            throw new UnauthorizedException("No Apple public key found for kid=" + kid);
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    private synchronized void refreshKeyCache() {
        try {
            String json = restTemplate.getForObject(APPLE_JWKS_URL, String.class);
            Map<String, Object> jwks = objectMapper.readValue(json, new TypeReference<>() {});
            List<Map<String, String>> keys = (List<Map<String, String>>) jwks.get("keys");

            ConcurrentHashMap<String, PublicKey> newCache = new ConcurrentHashMap<>();
            for (Map<String, String> jwk : keys) {
                String jwkKid = jwk.get("kid");
                PublicKey publicKey = buildRsaPublicKey(jwk.get("n"), jwk.get("e"));
                newCache.put(jwkKid, publicKey);
            }

            keyCache.clear();
            keyCache.putAll(newCache);
            cacheLoadedAt = System.currentTimeMillis();
            log.debug("Refreshed Apple JWKS cache: {} keys loaded", newCache.size());
        } catch (Exception e) {
            log.error("Failed to refresh Apple JWKS cache: {}", e.getMessage(), e);
            throw new UnauthorizedException("Could not fetch Apple public keys");
        }
    }

    private PublicKey buildRsaPublicKey(String n, String e) {
        try {
            byte[] nBytes = Base64.getUrlDecoder().decode(n);
            byte[] eBytes = Base64.getUrlDecoder().decode(e);
            RSAPublicKeySpec spec = new RSAPublicKeySpec(
                    new BigInteger(1, nBytes),
                    new BigInteger(1, eBytes)
            );
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to build RSA public key from Apple JWK", ex);
        }
    }

    public record AppleClaims(String sub, String email) {}
}
