// ========== GarminOAuthService.java ==========
package com.runnit.api.service;

import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarminOAuthService {

    private final UserRepository userRepository;
    private final User user;
    
    @Value("${garmin.oauth.consumer-key}")
    private String consumerKey;
    
    @Value("${garmin.oauth.consumer-secret}")
    private String consumerSecret;
    
    @Value("${garmin.oauth.callback-url}")
    private String callbackUrl;
    
    private static final String REQUEST_TOKEN_URL = "https://connectapi.garmin.com/oauth-service/oauth/request_token";
    private static final String AUTHORIZE_URL = "https://connect.garmin.com/oauthConfirm";
    private static final String ACCESS_TOKEN_URL = "https://connectapi.garmin.com/oauth-service/oauth/access_token";
    
    // Temporary storage for OAuth tokens during flow (use Redis in production)
    private final Map<String, OAuthTokenData> pendingTokens = new HashMap<>();

    /**
     * Step 1: Get authorization URL
     */
    public String getAuthorizationUrl(String userEmail) {
        try {
            OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
            OAuthProvider provider = new DefaultOAuthProvider(
                REQUEST_TOKEN_URL,
                ACCESS_TOKEN_URL,
                AUTHORIZE_URL
            );
            
            // Get request token
            String authUrl = provider.retrieveRequestToken(consumer, callbackUrl);
            
            // Store token data temporarily (keyed by request token)
            String requestToken = consumer.getToken();
            pendingTokens.put(requestToken, new OAuthTokenData(
                userEmail,
                consumer.getToken(),
                consumer.getTokenSecret()
            ));
            
            log.info("Generated Garmin auth URL for user: {}", userEmail);
            return authUrl;
            
        } catch (Exception e) {
            log.error("Failed to generate Garmin auth URL", e);
            throw new RuntimeException("Failed to initiate Garmin OAuth", e);
        }
    }

    /**
     * Step 2: Handle OAuth callback and exchange for access token
     */
    @Transactional
    public void handleCallback(String oauthToken, String oauthVerifier) {
        try {
            // Retrieve pending token data
            OAuthTokenData tokenData = pendingTokens.get(oauthToken);
            if (tokenData == null) {
                throw new RuntimeException("Invalid or expired OAuth token");
            }
            
            // Exchange for access token
            OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
            consumer.setTokenWithSecret(tokenData.getToken(), tokenData.getTokenSecret());
            
            OAuthProvider provider = new DefaultOAuthProvider(
                REQUEST_TOKEN_URL,
                ACCESS_TOKEN_URL,
                AUTHORIZE_URL
            );
            
            provider.retrieveAccessToken(consumer, oauthVerifier);
            
            // Save access token to user
            User user = userRepository.findByEmail(tokenData.getUserEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setGarminAccessToken(consumer.getToken());
            user.setGarminTokenSecret(consumer.getTokenSecret());
            userRepository.save(user);
            
            // Clean up pending token
            pendingTokens.remove(oauthToken);
            
            log.info("Successfully connected Garmin for user: {}", user.getEmail());
            
        } catch (Exception e) {
            log.error("Failed to handle Garmin callback", e);
            throw new RuntimeException("Failed to complete Garmin OAuth", e);
        }
    }

    public boolean isConnected(String userEmail) {
        return userRepository.findByEmail(userEmail)
            .map(user -> user.getGarminAccessToken() != null)
            .orElse(false);
    }

    @Transactional
    public void disconnect(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setGarminAccessToken(null);
        user.setGarminTokenSecret(null);
        userRepository.save(user);
        
        log.info("Disconnected Garmin for user: {}", userEmail);
    }
    
    private record OAuthTokenData(String userEmail, String token, String tokenSecret) {}
}