package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class GarminService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ObjectMapper objectMapper;
    private final AutoMomentService autoMomentService;

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
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

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
                .orElseThrow(() -> new ResourceNotFoundException("No user found for Garmin request token"));

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

        // Sync recent activities in background — non-fatal if it fails
        try {
            syncActivities(user);
        } catch (Exception e) {
            log.warn("Post-OAuth Garmin activity sync failed for user {}: {}", user.getId(), e.getMessage());
        }

        return frontendUrl + "/devices?garmin=connected";
    }

    /** Sync activities from Garmin for a user by ID */
    @Transactional
    public int syncActivities(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return syncActivities(user);
    }

    // 20 pages * 50 = 1000 activities per sync — safety cap, not an expected ceiling.
    private static final int MAX_SYNC_PAGES = 20;
    private static final int PAGE_SIZE = 50;

    @Transactional
    public int syncActivities(User user) {
        if (user.getGarminAccessToken() == null) return 0;

        try {
            OAuthConsumer consumer = new DefaultOAuthConsumer(consumerKey, consumerSecret);
            consumer.setTokenWithSecret(user.getGarminAccessToken(), user.getGarminAccessTokenSecret());

            // This endpoint's `start` is a pagination OFFSET (index into the user's full
            // activity list), not a date filter — passing an epoch-seconds value here (as
            // this code previously did) meant "skip ~1.7 billion activities," which is
            // almost certainly not what was intended and likely returned nothing useful
            // beyond whatever the server tolerated. There's no date-range param on this
            // endpoint, so this loop just pages through everything, newest first, until a
            // page comes back short (end of data) or the safety cap is hit — activities
            // older than 90 days will still get pulled in if a user has fewer than
            // MAX_SYNC_PAGES*PAGE_SIZE total activities, which is an acceptable trade
            // for actually fetching the recent ones correctly.
            int imported = 0;
            for (int page = 0; page < MAX_SYNC_PAGES; page++) {
                int offset = page * PAGE_SIZE;
                String urlStr = ACTIVITIES_URL + "?start=" + offset + "&limit=" + PAGE_SIZE;

                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                consumer.sign(conn);
                conn.connect();

                if (conn.getResponseCode() != 200) break;

                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                List<Map<String, Object>> activities = objectMapper.readValue(
                        sb.toString(), new TypeReference<>() {});
                if (activities.isEmpty()) break;

                for (Map<String, Object> act : activities) {
                    if (saveGarminActivity(user, act)) imported++;
                }

                if (activities.size() < PAGE_SIZE) break; // short page — no more data
            }

            user.setGarminLastSync(Instant.now());
            userRepository.save(user);
            return imported;

        } catch (Exception e) {
            log.warn("Garmin activity sync failed for user {}: {}", user.getId(), e.getMessage());
            return 0;
        }
    }

    /** Disconnect Garmin — clear all tokens */
    @Transactional
    public void disconnect(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
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
                .performedAt(parseGarminStart(act))
                .build();

        activityRepository.save(activity);
        try {
            autoMomentService.onActivityRecorded(activity);
        } catch (Exception e) {
            log.warn("Auto-moment creation failed for Garmin activity {}: {}", externalId, e.getMessage());
        }
        return true;
    }

    /**
     * Without this, every activity pulled by this REST sync path (initial
     * OAuth-connect backfill and the manual "Sync Now" button) got stamped
     * with the sync moment instead of its real workout date — the whole
     * 90-day backlog would show up "Just now" and bury everything else in
     * the feed. GarminWebhookService's real-time push path already does
     * this correctly; this was the one path that got missed.
     */
    private LocalDateTime parseGarminStart(Map<String, Object> act) {
        Object startRaw = act.get("startTimeInSeconds");
        if (!(startRaw instanceof Number)) return null;
        long offsetSeconds = 0;
        Object offsetRaw = act.get("startTimeOffsetInSeconds");
        if (offsetRaw instanceof Number n) offsetSeconds = n.longValue();
        return LocalDateTime.ofEpochSecond(((Number) startRaw).longValue(), 0, ZoneOffset.ofTotalSeconds((int) offsetSeconds));
    }

    private Activity.SportType mapSportType(String type) {
        if (type == null) return Activity.SportType.OTHER;
        String lower = type.toLowerCase();
        if (lower.contains("run") || lower.contains("trail")) return Activity.SportType.RUN;
        if (lower.contains("cycl") || lower.contains("bike") || lower.contains("ride")) return Activity.SportType.BIKE;
        if (lower.contains("swim")) return Activity.SportType.SWIM;
        if (lower.contains("hike")) return Activity.SportType.HIKE;
        if (lower.contains("walk")) return Activity.SportType.WALK;
        if (lower.contains("strength")) return Activity.SportType.STRENGTH;
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
