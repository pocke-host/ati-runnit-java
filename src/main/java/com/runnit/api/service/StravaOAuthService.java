// ========== StravaOAuthService.java ==========
package com.runnit.api.service;

import com.runnit.api.dto.StravaTokenResponseDTO;
import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StravaOAuthService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    
    @Value("${strava.oauth.client-id}")
    private String clientId;
    
    @Value("${strava.oauth.client-secret}")
    private String clientSecret;
    
    @Value("${strava.oauth.callback-url}")
    private String callbackUrl;
    
    @Value("${strava.webhook.callback-url}")
    private String webhookCallbackUrl;
    
    @Value("${strava.webhook.verify-token}")
    private String webhookVerifyToken;
    
    private static final String AUTHORIZE_URL = "https://www.strava.com/oauth/authorize";
    private static final String TOKEN_URL = "https://www.strava.com/oauth/token";
    private static final String WEBHOOK_SUBSCRIPTION_URL = "https://www.strava.com/api/v3/push_subscriptions";
    
    // Store state tokens temporarily (use Redis in production)
    private final Map<String, String> stateToUserEmail = new HashMap<>();

    /**
     * Step 1: Generate authorization URL
     */
    public String getAuthorizationUrl(String userEmail) {
        // Generate random state token for CSRF protection
        String state = UUID.randomUUID().toString();
        stateToUserEmail.put(state, userEmail);
        
        String authUrl = String.format(
            "%s?client_id=%s&redirect_uri=%s&response_type=code&scope=activity:read_all,activity:write&state=%s",
            AUTHORIZE_URL,
            clientId,
            callbackUrl,
            state
        );
        
        log.info("Generated Strava auth URL for user: {}", userEmail);
        return authUrl;
    }

    /**
     * Step 2: Exchange authorization code for access token
     */
    @Transactional
    public void handleCallback(String authorizationCode, String state) {
        try {
            // Verify state token
            String userEmail = stateToUserEmail.get(state);
            if (userEmail == null) {
                throw new RuntimeException("Invalid or expired state token");
            }
            
            // Exchange code for access token
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("code", authorizationCode);
            params.add("grant_type", "authorization_code");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<StravaTokenResponseDTO> response = restTemplate.postForEntity(
                TOKEN_URL,
                request,
                StravaTokenResponseDTO.class
            );
            
            StravaTokenResponseDTO tokenResponse = response.getBody();
            if (tokenResponse == null) {
                throw new RuntimeException("Failed to get Strava access token");
            }
            
            // Save tokens to user
            User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setStravaAccessToken(tokenResponse.getAccessToken());
            user.setStravaRefreshToken(tokenResponse.getRefreshToken());
            user.setStravaAthleteId(tokenResponse.getAthlete().getId());
            user.setStravaTokenExpiresAt(
                LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn())
            );
            
            userRepository.save(user);
            
            // Clean up state token
            stateToUserEmail.remove(state);
            
            log.info("Successfully connected Strava for user: {} (athlete ID: {})", 
                user.getEmail(), tokenResponse.getAthlete().getId());
            
        } catch (Exception e) {
            log.error("Failed to handle Strava callback", e);
            throw new RuntimeException("Failed to complete Strava OAuth", e);
        }
    }

    /**
     * Refresh Strava access token if expired
     */
    @Transactional
    public void refreshAccessToken(User user) {
        try {
            log.info("Refreshing Strava access token for user: {}", user.getEmail());
            
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", user.getStravaRefreshToken());
            params.add("grant_type", "refresh_token");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<StravaTokenResponseDTO> response = restTemplate.postForEntity(
                TOKEN_URL,
                request,
                StravaTokenResponseDTO.class
            );
            
            StravaTokenResponseDTO tokenResponse = response.getBody();
            if (tokenResponse == null) {
                throw new RuntimeException("Failed to refresh Strava token");
            }
            
            user.setStravaAccessToken(tokenResponse.getAccessToken());
            user.setStravaRefreshToken(tokenResponse.getRefreshToken());
            user.setStravaTokenExpiresAt(
                LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn())
            );
            
            userRepository.save(user);
            
            log.info("Successfully refreshed Strava token for user: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to refresh Strava token", e);
            throw new RuntimeException("Failed to refresh Strava token", e);
        }
    }

    public boolean isConnected(String userEmail) {
        return userRepository.findByEmail(userEmail)
            .map(user -> user.getStravaAccessToken() != null)
            .orElse(false);
    }

    @Transactional
    public void disconnect(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // TODO: Deauthorize on Strava side
        
        user.setStravaAccessToken(null);
        user.setStravaRefreshToken(null);
        user.setStravaAthleteId(null);
        user.setStravaTokenExpiresAt(null);
        
        userRepository.save(user);
        
        log.info("Disconnected Strava for user: {}", userEmail);
    }

    /**
     * Subscribe to Strava webhook events
     * This should be called once when setting up the app
     */
    public void subscribeToWebhook() {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("callback_url", webhookCallbackUrl);
            params.add("verify_token", webhookVerifyToken);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                WEBHOOK_SUBSCRIPTION_URL,
                request,
                Map.class
            );
            
            log.info("Strava webhook subscription response: {}", response.getBody());
            
        } catch (Exception e) {
            log.error("Failed to subscribe to Strava webhook", e);
            throw new RuntimeException("Failed to subscribe to Strava webhook", e);
        }
    }
}