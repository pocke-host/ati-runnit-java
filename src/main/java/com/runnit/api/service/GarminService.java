package com.runnit.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Garmin Connect Developer Program integration.
 *
 * Garmin's current program uses OAuth 2.0 Authorization Code + PKCE. The
 * access token is used for push delivery and for Training API writes; Starter
 * access does not provide historical activity pull, so syncActivities is an
 * explicit no-op with a truthful status response.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GarminService {

    private static final String AUTHORIZATION_URL = "https://connect.garmin.com/oauth2Confirm";
    private static final String TOKEN_URL = "https://diauth.garmin.com/di-oauth2-service/oauth/token";
    private static final String USER_ID_URL = "https://apis.garmin.com/wellness-api/rest/user/id";
    private static final String PERMISSIONS_URL = "https://apis.garmin.com/wellness-api/rest/user/permissions";
    private static final String REGISTRATION_URL = "https://apis.garmin.com/wellness-api/rest/user/registration";
    private static final long EXPIRY_SKEW_SECONDS = 600L;

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${garmin.client.id:}")
    private String clientId;

    @Value("${garmin.client.secret:}")
    private String clientSecret;

    @Value("${garmin.redirect.uri:https://ati-runnit-java.onrender.com/api/garmin/oauth/callback}")
    private String redirectUri;

    @Value("${garmin.training.workout-url:https://apis.garmin.com/training-api/workout}")
    private String trainingWorkoutUrl;

    @Value("${app.frontend.url:https://runnit.live}")
    private String frontendUrl;

    /** Start OAuth2 PKCE and bind the state/verifier to the authenticated user. */
    @Transactional
    public String buildAuthorizationUrl(Long userId) {
        requireConfigured();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String verifier = generateVerifier();
        String state = randomUrlSafe(32);
        user.setGarminOauthState(state);
        user.setGarminOauthCodeVerifier(verifier);
        userRepository.save(user);

        return UriComponentsBuilder.fromUriString(AUTHORIZATION_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("code_challenge", codeChallenge(verifier))
                .queryParam("code_challenge_method", "S256")
                .queryParam("redirect_uri", redirectUri)
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
    }

    /** Exchange the one-time authorization code, then resolve Garmin identity and permissions. */
    @Transactional
    public String handleCallback(String code, String state) {
        requireConfigured();
        User user = userRepository.findByGarminOauthState(state)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid Garmin OAuth state"));
        String verifier = user.getGarminOauthCodeVerifier();
        if (verifier == null || verifier.isBlank()) {
            throw new IllegalStateException("Garmin OAuth verifier is missing or expired");
        }

        Map<String, Object> token = exchangeCode(code, verifier);
        storeTokenResponse(user, token, true);
        user.setGarminOauthState(null);
        user.setGarminOauthCodeVerifier(null);
        // Clear fields from the retired OAuth1 integration so status cannot report
        // a stale legacy connection after a successful OAuth2 connection.
        user.setGarminAccessTokenSecret(null);
        user.setGarminRequestToken(null);
        user.setGarminRequestTokenSecret(null);
        userRepository.save(user);

        resolveIdentityAndPermissions(user);
        userRepository.save(user);
        return frontendUrl + "/devices?garmin=connected";
    }

    /**
     * Check the Garmin connection and retry the post-consent identity lookup.
     *
     * Starter access is push-based, so this does not pull activities like WHOOP
     * does. It validates the token and repairs a transient identity-link failure;
     * new Garmin activities arrive through the configured webhook after the user
     * syncs Garmin Connect.
     */
    @Transactional
    public int syncActivities(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getGarminAccessToken() == null && user.getGarminRefreshToken() == null) return 0;
        getValidAccessToken(user);
        if (user.getGarminUserId() == null || user.getGarminUserId().isBlank()) {
            resolveIdentityAndPermissions(user);
            userRepository.save(user);
        }
        return 0;
    }

    /** Publish a planned workout through the Garmin Training API. */
    @Transactional
    public Map<String, Object> publishWorkout(Long userId, Map<String, Object> workout) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String accessToken = getValidAccessToken(user);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                trainingWorkoutUrl,
                HttpMethod.POST,
                new HttpEntity<>(workout, bearerHeaders(accessToken)),
                new ParameterizedTypeReference<>() {}
        );
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("published", response.getStatusCode().is2xxSuccessful());
        result.put("status", response.getStatusCode().value());
        if (response.getBody() != null) result.putAll(response.getBody());
        return result;
    }

    /** Deregister the user at Garmin when possible, then always clear local credentials. */
    @Transactional
    public void disconnect(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getGarminAccessToken() != null) {
            try {
                restTemplate.exchange(REGISTRATION_URL, HttpMethod.DELETE,
                        new HttpEntity<>(bearerHeaders(getValidAccessToken(user))), Void.class);
            } catch (Exception e) {
                log.warn("Garmin deregistration failed for user {}: {}", userId, e.getMessage());
            }
        }
        clearCredentials(user);
        userRepository.save(user);
    }

    public Map<String, Object> getStatus(Long userId) {
        return userRepository.findById(userId).map(user -> {
            Map<String, Object> status = new LinkedHashMap<>();
            boolean connected = user.getGarminAccessToken() != null || user.getGarminRefreshToken() != null;
            boolean expiredWithoutRefresh = user.getGarminTokenExpiresAt() != null
                    && user.getGarminTokenExpiresAt() <= Instant.now().getEpochSecond()
                    && (user.getGarminRefreshToken() == null || user.getGarminRefreshToken().isBlank());
            status.put("connected", connected);
            status.put("needsReconnect", connected && (user.getGarminAccessToken() == null || expiredWithoutRefresh));
            status.put("lastSync", user.getGarminLastSync() != null ? user.getGarminLastSync().toString() : null);
            status.put("deliveryMode", "push");
            status.put("historicalImportSupported", false);
            status.put("userIdLinked", user.getGarminUserId() != null);
            status.put("syncState", syncState(user));
            status.put("permissions", parsePermissions(user.getGarminPermissions()));
            return status;
        }).orElseGet(() -> {
            Map<String, Object> status = new LinkedHashMap<>();
            status.put("connected", false);
            status.put("needsReconnect", false);
            status.put("lastSync", null);
            status.put("deliveryMode", "push");
            status.put("historicalImportSupported", false);
            status.put("userIdLinked", false);
            status.put("syncState", "disconnected");
            status.put("permissions", java.util.List.of());
            return status;
        });
    }

    public String getFrontendUrl() {
        return frontendUrl;
    }

    private Map<String, Object> exchangeCode(String code, String verifier) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("code_verifier", verifier);
        form.add("redirect_uri", redirectUri);
        return postToken(form);
    }

    private Map<String, Object> refreshToken(User user) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("refresh_token", user.getGarminRefreshToken());
        return postToken(form);
    }

    private Map<String, Object> postToken(MultiValueMap<String, String> form) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST, new HttpEntity<>(form, headers),
                new ParameterizedTypeReference<>() {}
        );
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null
                || response.getBody().get("access_token") == null) {
            throw new IllegalStateException("Garmin token exchange did not return an access token");
        }
        return response.getBody();
    }

    private synchronized String getValidAccessToken(User user) {
        if (user.getGarminAccessToken() != null
                && (user.getGarminTokenExpiresAt() == null
                || user.getGarminTokenExpiresAt() > Instant.now().getEpochSecond() + EXPIRY_SKEW_SECONDS)) {
            return user.getGarminAccessToken();
        }
        if (user.getGarminRefreshToken() == null || user.getGarminRefreshToken().isBlank()) {
            throw new IllegalStateException("Garmin connection needs to be reconnected");
        }
        Map<String, Object> token = refreshToken(user);
        storeTokenResponse(user, token, false);
        userRepository.save(user);
        return user.getGarminAccessToken();
    }

    private void storeTokenResponse(User user, Map<String, Object> token, boolean requireRefreshToken) {
        String accessToken = stringValue(token, "access_token");
        String refreshToken = stringValue(token, "refresh_token");
        if (accessToken == null) throw new IllegalStateException("Garmin access token is missing");
        if (requireRefreshToken && (refreshToken == null || refreshToken.isBlank())) {
            throw new IllegalStateException("Garmin refresh token is missing");
        }
        user.setGarminAccessToken(accessToken);
        if (refreshToken != null && !refreshToken.isBlank()) user.setGarminRefreshToken(refreshToken);
        Long expiresIn = longValue(token, "expires_in");
        if (expiresIn != null) user.setGarminTokenExpiresAt(Instant.now().getEpochSecond() + expiresIn);
    }

    private void resolveIdentityAndPermissions(User user) {
        try {
            String accessToken = getValidAccessToken(user);
            ResponseEntity<Map<String, Object>> identity = restTemplate.exchange(
                    USER_ID_URL, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)),
                    new ParameterizedTypeReference<>() {}
            );
            if (identity.getBody() != null) {
                String userId = stringValue(identity.getBody(), "userId");
                if (userId != null) user.setGarminUserId(userId);
            }
            ResponseEntity<Object> permissions = restTemplate.exchange(
                    PERMISSIONS_URL, HttpMethod.GET, new HttpEntity<>(bearerHeaders(accessToken)), Object.class);
            if (permissions.getBody() != null) user.setGarminPermissions(objectMapper.writeValueAsString(permissions.getBody()));
        } catch (Exception e) {
            log.warn("Garmin identity/permissions lookup failed for user {}: {}", user.getId(), e.getMessage());
            // Keep the OAuth connection; the webhook can still identify the user by token
            // while the next status/refresh attempt retries identity resolution.
        }
    }

    private HttpHeaders bearerHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private void clearCredentials(User user) {
        user.setGarminAccessToken(null);
        user.setGarminRefreshToken(null);
        user.setGarminTokenExpiresAt(null);
        user.setGarminOauthState(null);
        user.setGarminOauthCodeVerifier(null);
        user.setGarminUserId(null);
        user.setGarminPermissions(null);
        user.setGarminAccessTokenSecret(null);
        user.setGarminRequestToken(null);
        user.setGarminRequestTokenSecret(null);
        user.setGarminLastSync(null);
    }

    private void requireConfigured() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Garmin OAuth is not configured");
        }
    }

    private String syncState(User user) {
        if (user.getGarminAccessToken() == null && user.getGarminRefreshToken() == null) return "disconnected";
        if (user.getGarminUserId() == null || user.getGarminUserId().isBlank()) return "identity_pending";
        if (user.getGarminLastSync() == null) return "waiting_for_delivery";
        return user.getGarminLastSync().isBefore(Instant.now().minusSeconds(72 * 60 * 60L))
                ? "stale" : "healthy";
    }

    private String generateVerifier() {
        return randomUrlSafe(64);
    }

    private String randomUrlSafe(int bytes) {
        byte[] value = new byte[bytes];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String codeChallenge(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(verifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    private String stringValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        return value == null ? null : value.toString();
    }

    private Long longValue(Map<String, Object> values, String key) {
        Object value = values.get(key);
        if (value instanceof Number number) return number.longValue();
        if (value == null) return null;
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Object parsePermissions(String raw) {
        if (raw == null || raw.isBlank()) return java.util.List.of();
        try {
            return objectMapper.readValue(raw, new TypeReference<>() {});
        } catch (Exception ignored) {
            return raw;
        }
    }
}
