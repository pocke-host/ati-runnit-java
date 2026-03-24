package com.runnit.api.security;

import com.runnit.api.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * Validates Google ID tokens by calling Google's tokeninfo endpoint.
 * No additional dependencies required — uses RestTemplate + Google's REST API.
 *
 * Docs: https://developers.google.com/identity/sign-in/web/backend-auth#verify-the-integrity-of-the-id-token
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleTokenValidator {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo";

    private final RestTemplate restTemplate;

    @Value("${google.client.id:}")
    private String googleClientId;

    /**
     * Validates a Google ID token and returns the verified claims.
     *
     * @param idToken the Google ID token from the client
     * @return GoogleClaims with sub, email, name, picture
     * @throws UnauthorizedException if the token is invalid or aud doesn't match
     */
    @SuppressWarnings("unchecked")
    public GoogleClaims validate(String idToken) {
        String url = UriComponentsBuilder.fromHttpUrl(TOKENINFO_URL)
                .queryParam("id_token", idToken)
                .toUriString();

        Map<String, Object> claims;
        try {
            claims = restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.warn("Google tokeninfo request failed: {}", e.getMessage());
            throw new UnauthorizedException("Invalid Google ID token");
        }

        if (claims == null || claims.containsKey("error")) {
            throw new UnauthorizedException("Invalid Google ID token");
        }

        // Verify the token was issued for our app
        String aud = (String) claims.get("aud");
        if (googleClientId != null && !googleClientId.isBlank() && !googleClientId.equals(aud)) {
            log.warn("Google token aud mismatch: expected={}, got={}", googleClientId, aud);
            throw new UnauthorizedException("Google token audience mismatch");
        }

        // Require verified email
        if (!"true".equals(claims.get("email_verified"))) {
            throw new UnauthorizedException("Google account email is not verified");
        }

        return new GoogleClaims(
                (String) claims.get("sub"),
                (String) claims.get("email"),
                (String) claims.get("name"),
                (String) claims.get("picture")
        );
    }

    public record GoogleClaims(String sub, String email, String name, String picture) {}
}
