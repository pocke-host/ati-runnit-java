package com.runnit.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.model.User;
import com.runnit.api.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${google.client.id:}")
    private String googleClientId;

    @Value("${google.client.secret:}")
    private String googleClientSecret;

    @Value("${google.redirect.uri:}")
    private String googleRedirectUri;

    @Value("${apple.client.id:}")
    private String appleClientId;

    @Value("${apple.redirect.uri:}")
    private String appleRedirectUri;

    @Value("${app.frontend.url:https://runnit.live}")
    private String frontendUrl;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    // ── Google ────────────────────────────────────────────────────────────────

    @GetMapping("/google")
    public void initiateGoogleLogin(HttpServletResponse response) throws IOException {
        String state = generateState();
        String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + encode(googleClientId)
                + "&redirect_uri=" + encode(googleRedirectUri)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&state=" + state
                + "&access_type=offline"
                + "&prompt=select_account";
        response.sendRedirect(url);
    }

    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            HttpServletResponse response) throws IOException {
        try {
            // Exchange authorization code for access token
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("code", code);
            params.add("client_id", googleClientId);
            params.add("client_secret", googleClientSecret);
            params.add("redirect_uri", googleRedirectUri);
            params.add("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(params, headers);

            ResponseEntity<String> tokenResponse = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", tokenRequest, String.class);

            JsonNode tokenJson = objectMapper.readTree(tokenResponse.getBody());
            String accessToken = tokenJson.get("access_token").asText();

            // Fetch user info from Google
            HttpHeaders userHeaders = new HttpHeaders();
            userHeaders.setBearerAuth(accessToken);
            HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

            ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                    "https://www.googleapis.com/oauth2/v3/userinfo",
                    HttpMethod.GET, userRequest, String.class);

            JsonNode userJson = objectMapper.readTree(userInfoResponse.getBody());
            String providerId = userJson.get("sub").asText();
            String email      = userJson.path("email").asText(null);
            String displayName = userJson.path("name").asText(email);
            String avatarUrl  = userJson.path("picture").asText(null);

            Map<String, Object> result = authService.handleOAuthLogin(
                    User.AuthProvider.GOOGLE, providerId, email, displayName, avatarUrl);

            setJwtCookie(response, (String) result.get("token"));
            response.sendRedirect(frontendUrl + "/oauth-callback");
        } catch (Exception e) {
            log.error("Google OAuth callback failed", e);
            response.sendRedirect(frontendUrl + "/oauth-callback?error=google_auth_failed");
        }
    }

    // ── Apple ─────────────────────────────────────────────────────────────────

    @GetMapping("/apple")
    public void initiateAppleLogin(HttpServletResponse response) throws IOException {
        String state = generateState();
        String url = "https://appleid.apple.com/auth/authorize"
                + "?client_id=" + encode(appleClientId)
                + "&redirect_uri=" + encode(appleRedirectUri)
                + "&response_type=code%20id_token"
                + "&response_mode=form_post"
                + "&scope=" + encode("name email")
                + "&state=" + state;
        response.sendRedirect(url);
    }

    // Apple uses form_post so the callback is a POST with form-encoded body
    @PostMapping("/apple/callback")
    public void appleCallback(
            @RequestParam String code,
            @RequestParam(name = "id_token", required = false) String idToken,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String user,
            HttpServletResponse response) throws IOException {
        try {
            if (idToken == null) {
                response.sendRedirect(frontendUrl + "/oauth-callback?error=missing_token");
                return;
            }

            // Decode the id_token JWT payload (middle segment) to extract sub + email.
            // TODO: verify signature against Apple's public keys before going live.
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                response.sendRedirect(frontendUrl + "/oauth-callback?error=invalid_token");
                return;
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            JsonNode claims = objectMapper.readTree(payload);
            String providerId = claims.get("sub").asText();
            String email = claims.path("email").asText(null);

            // Apple only sends the user's name on the very first authorization
            String displayName = email;
            if (user != null) {
                try {
                    JsonNode userJson = objectMapper.readTree(user);
                    String first = userJson.path("name").path("firstName").asText("");
                    String last  = userJson.path("name").path("lastName").asText("");
                    if (!first.isBlank() || !last.isBlank()) {
                        displayName = (first + " " + last).trim();
                    }
                } catch (Exception parseEx) {
                    log.warn("Could not parse Apple user name payload", parseEx);
                }
            }

            Map<String, Object> result = authService.handleOAuthLogin(
                    User.AuthProvider.APPLE, providerId, email, displayName, null);

            setJwtCookie(response, (String) result.get("token"));
            response.sendRedirect(frontendUrl + "/oauth-callback");
        } catch (Exception e) {
            log.error("Apple OAuth callback failed", e);
            response.sendRedirect(frontendUrl + "/oauth-callback?error=apple_auth_failed");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String generateState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String encode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private void setJwtCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("jwt", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtExpiration / 1000));
        response.addCookie(cookie);
    }
}
