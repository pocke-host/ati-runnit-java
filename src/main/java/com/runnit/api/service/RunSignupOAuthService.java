package com.runnit.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RunSignupOAuthService {
    private static final String AUTHORIZE_URL = "https://runsignup.com/Profile/OAuth2/RequestGrant";
    private static final String REDEEM_URL = "https://runsignup.com/rest/v2/auth/auth-code-redemption.json";
    private static final String REFRESH_URL = "https://runsignup.com/rest/v2/auth/refresh-token.json";
    private static final String SCOPE = "rsu_api_write";
    private final UserRepository users;
    private final RestTemplate http;
    private final ObjectMapper mapper;
    private final SecureRandom random = new SecureRandom();
    @Value("${runsignup.oauth.client-id:}") private String clientId;
    @Value("${runsignup.oauth.client-secret:}") private String clientSecret;
    @Value("${runsignup.oauth.redirect-uri:https://ati-runnit-java.onrender.com/api/integrations/runsignup/oauth/callback}") private String redirectUri;
    @Value("${app.frontend.url:https://runnit.live}") private String frontendUrl;

    @Transactional
    public String connect(Long userId) {
        requireConfigured();
        User user = users.findById(userId).orElseThrow();
        String state = randomString(48);
        String verifier = randomString(64);
        user.setRunSignupOauthState(state);
        user.setRunSignupOauthVerifier(verifier);
        users.save(user);
        String challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier));
        return AUTHORIZE_URL + "?response_type=code&client_id=" + enc(clientId)
                + "&scope=" + enc(SCOPE) + "&redirect_uri=" + enc(redirectUri)
                + "&state=" + enc(state) + "&code_challenge_method=S256&code_challenge=" + enc(challenge);
    }

    @Transactional
    public String callback(String code, String state) {
        requireConfigured();
        User user = users.findByRunSignupOauthState(state)
                .orElseThrow(() -> new IllegalStateException("RunSignup authorization expired"));
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code"); body.add("client_id", clientId);
        body.add("client_secret", clientSecret); body.add("redirect_uri", redirectUri);
        body.add("code", code); body.add("code_verifier", user.getRunSignupOauthVerifier());
        saveTokens(user, exchange(REDEEM_URL, body));
        user.setRunSignupOauthState(null); user.setRunSignupOauthVerifier(null); users.save(user);
        return frontendUrl + "/races?runsignup=connected";
    }

    public Map<String, Object> status(Long userId) {
        User u = users.findById(userId).orElseThrow();
        return Map.of("connected", u.getRunSignupRefreshToken() != null,
                "expiresAt", u.getRunSignupTokenExpiresAt() == null ? 0 : u.getRunSignupTokenExpiresAt());
    }

    public String accessToken(User user) {
        if (user.getRunSignupAccessToken() == null) throw new IllegalStateException("RunSignup is not connected");
        if (user.getRunSignupTokenExpiresAt() != null && Instant.now().getEpochSecond() < user.getRunSignupTokenExpiresAt() - 60) return user.getRunSignupAccessToken();
        if (user.getRunSignupRefreshToken() == null) throw new IllegalStateException("RunSignup authorization expired");
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token"); body.add("client_id", clientId); body.add("client_secret", clientSecret); body.add("refresh_token", user.getRunSignupRefreshToken());
        saveTokens(user, exchange(REFRESH_URL, body)); users.save(user); return user.getRunSignupAccessToken();
    }

    private Map<String, Object> exchange(String url, MultiValueMap<String, String> body) {
        HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try { return mapper.readValue(http.postForEntity(url, new HttpEntity<>(body, h), String.class).getBody(), new TypeReference<>() {}); }
        catch (Exception e) { throw new IllegalStateException("RunSignup OAuth token exchange failed", e); }
    }
    private void saveTokens(User u, Map<String, Object> t) {
        u.setRunSignupAccessToken((String) t.get("access_token"));
        if (t.get("refresh_token") != null) u.setRunSignupRefreshToken((String) t.get("refresh_token"));
        long expires = t.get("expires_in") instanceof Number ? ((Number) t.get("expires_in")).longValue() : 2678400;
        u.setRunSignupTokenExpiresAt(Instant.now().getEpochSecond() + expires);
    }
    private void requireConfigured() { if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) throw new IllegalStateException("RunSignup OAuth is not configured"); }
    private String randomString(int length) { String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"; StringBuilder out = new StringBuilder(length); for (int i=0;i<length;i++) out.append(chars.charAt(random.nextInt(chars.length()))); return out.toString(); }
    private byte[] sha256(String value) { try { return java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.US_ASCII)); } catch (Exception e) { throw new IllegalStateException(e); } }
    private String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
}
