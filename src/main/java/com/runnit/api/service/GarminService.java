package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import oauth.signpost.OAuthConsumer;
import oauth.signpost.OAuthProvider;
import oauth.signpost.basic.DefaultOAuthConsumer;
import oauth.signpost.basic.DefaultOAuthProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class GarminService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ObjectMapper objectMapper;

    @Value("${garmin.consumer.key:}")
    private String consumerKey;

    @Value("${garmin.consumer.secret:}")
    private String consumerSecret;

    @Value("${garmin.callback.uri:https://ati-runnit-java.onrender.com/api/garmin/oauth/callback}")
    private String callbackUri;

    @Value("${app.frontend.url:https://runnit.live}")
    private String frontendUrl;

    private static final String REQUEST_TOKEN_URL = "https://connectapi.garmin.com/oauth-service/oauth/request_token";
    private static final String ACCESS_TOKEN_URL  = "https://connectapi.garmin.com/oauth-service/oauth/access_token";
    private static final String AUTHORIZE_URL     = "https://connect.garmin.com/oauthConfirm";
    private static final String ACTIVITIES_URL    = "https://connectapi.garmin.com/activitylist-service/activities/search/activities";

    /** Step 1: Get request token and return Garmin authorization URL */
    @Transactional
    public String buildAuthorizationUrl(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
        OAuthProvider provider = new DefaultOAuthProvider(REQUEST_TOKEN_URL, ACCESS_TOKEN_URL, AUTHORIZE_URL);

        try {
            String authUrl = provider.retrieveRequestToken(consumer, callbackUri);
            user.setGarminRequestToken(consumer.getToken());
            user.setGarminRequestTokenSecret(consumer.getTokenSecret());
            userRepository.save(user);
            return authUrl;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Garmin request token: " + e.getMessage(), e);
        }
    }

    /** Step 2: Exchange request token for access token after user authorizes */
    @Transactional
    public String handleCallback(String oauthToken, String oauthVerifier) {
        User user = userRepository.findByGarminRequestToken(oauthToken)
                .orElseThrow(() -> new RuntimeException("No user found for Garmin request token"));

        OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
        consumer.setTokenWithSecret(user.getGarminRequestToken(), user.getGarminRequestTokenSecret());
        OAuthProvider provider = new DefaultOAuthProvider(REQUEST_TOKEN_URL, ACCESS_TOKEN_URL, AUTHORIZE_URL);

        try {
            provider.retrieveAccessToken(consumer, oauthVerifier);
            user.setGarminAccessToken(consumer.getToken());
            user.setGarminAccessTokenSecret(consumer.getTokenSecret());
            user.setGarminRequestToken(null);
            user.setGarminRequestTokenSecret(null);
            userRepository.save(user);
        } catch (Exception e) {
            return frontendUrl + "/devices?error=garmin_token_failed";
        }

        // Sync recent activities in background
        try {
            syncActivities(user);
        } catch (Exception ignored) {}

        return frontendUrl + "/devices?garmin=connected";
    }

    /** Sync activities from Garmin for a user by ID */
    @Transactional
    public int syncActivities(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return syncActivities(user);
    }

    @Transactional
    public int syncActivities(User user) {
        if (user.getGarminAccessToken() == null) return 0;

        try {
            OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
            consumer.setTokenWithSecret(user.getGarminAccessToken(), user.getGarminAccessTokenSecret());

            // Fetch activities from the last 90 days (limit 50)
            long start = Instant.now().minusSeconds(90L * 24 * 3600).getEpochSecond();
            String urlStr = ACTIVITIES_URL + "?start=" + start + "&limit=50";

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            consumer.sign(conn);
            conn.connect();

            if (conn.getResponseCode() != 200) return 0;

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            reader.close();

            List<Map<String, Object>> activities = objectMapper.readValue(
                    sb.toString(), new TypeReference<>() {});

            int imported = 0;
            for (Map<String, Object> act : activities) {
                if (saveGarminActivity(user, act)) imported++;
            }

            user.setGarminLastSync(Instant.now());
            userRepository.save(user);
            return imported;

        } catch (Exception e) {
            return 0;
        }
    }

    /** Disconnect Garmin — clear all tokens */
    @Transactional
    public void disconnect(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setGarminAccessToken(null);
        user.setGarminAccessTokenSecret(null);
        user.setGarminRequestToken(null);
        user.setGarminRequestTokenSecret(null);
        user.setGarminLastSync(null);
        userRepository.save(user);
    }

    public Map<String, Object> getStatus(Long userId) {
        return userRepository.findById(userId).map(u -> {
            Map<String, Object> status = new HashMap<>();
            status.put("connected", u.getGarminAccessToken() != null);
            status.put("lastSync", u.getGarminLastSync() != null ? u.getGarminLastSync().toString() : null);
            return status;
        }).orElse(Map.of("connected", false, "lastSync", null));
    }

    public String getFrontendUrl() { return frontendUrl; }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private boolean saveGarminActivity(User user, Map<String, Object> act) {
        String externalId = "garmin_" + act.get("activityId");
        if (activityRepository.existsByUserIdAndExternalId(user.getId(), externalId)) return false;

        Activity activity = Activity.builder()
                .user(user)
                .externalId(externalId)
                .source(Activity.Source.GARMIN)
                .sportType(mapSportType(getString(act, "activityType")))
                .durationSeconds(getInt(act, "duration"))
                .distanceMeters(getInt(act, "distance"))
                .elevationGain(getInt(act, "elevationGain"))
                .calories(getInt(act, "calories"))
                .averageHeartRate(getInt(act, "averageHR"))
                .maxHeartRate(getInt(act, "maxHR"))
                .averagePace(getDouble(act, "averageSpeed"))
                .build();

        activityRepository.save(activity);
        return true;
    }

    private Activity.SportType mapSportType(String type) {
        if (type == null) return Activity.SportType.OTHER;
        String lower = type.toLowerCase();
        if (lower.contains("run") || lower.contains("trail")) return Activity.SportType.RUN;
        if (lower.contains("cycl") || lower.contains("bike") || lower.contains("ride")) return Activity.SportType.BIKE;
        if (lower.contains("swim")) return Activity.SportType.SWIM;
        if (lower.contains("hike")) return Activity.SportType.HIKE;
        if (lower.contains("walk")) return Activity.SportType.WALK;
        return Activity.SportType.OTHER;
    }

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).intValue() : null;
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : null;
    }
}
