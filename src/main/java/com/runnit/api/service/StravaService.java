package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StravaService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    @Value("${strava.client.id}")
    private String clientId;

    @Value("${strava.client.secret}")
    private String clientSecret;

    @Value("${strava.redirect.uri}")
    private String redirectUri;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String STRAVA_TOKEN_URL = "https://www.strava.com/oauth/token";
    private static final String STRAVA_ACTIVITIES_URL = "https://www.strava.com/api/v3/athlete/activities";
    private static final String STRAVA_ACTIVITY_URL = "https://www.strava.com/api/v3/activities/";

    private final RestTemplate restTemplate = new RestTemplate();

    /** Generate Strava OAuth authorization URL, store state on user */
    @Transactional
    public String buildAuthorizationUrl(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String state = UUID.randomUUID().toString();
        user.setStravaOauthState(state);
        userRepository.save(user);

        return "https://www.strava.com/oauth/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=activity:read_all" +
                "&approval_prompt=auto" +
                "&state=" + state;
    }

    /** Exchange auth code for tokens, sync recent activities, return frontend redirect URL */
    @Transactional
    public String handleCallback(String code, String state) {
        User user = userRepository.findByStravaOauthState(state)
                .orElseThrow(() -> new RuntimeException("Invalid OAuth state"));

        // Exchange code for tokens
        Map<String, Object> tokenResponse = exchangeCodeForToken(code);
        if (tokenResponse == null) {
            return frontendUrl + "/devices?error=token_exchange_failed";
        }

        // Store tokens
        user.setStravaAccessToken((String) tokenResponse.get("access_token"));
        user.setStravaRefreshToken((String) tokenResponse.get("refresh_token"));
        user.setStravaTokenExpiresAt(((Number) tokenResponse.get("expires_at")).longValue());
        user.setStravaOauthState(null);

        Map<String, Object> athlete = (Map<String, Object>) tokenResponse.get("athlete");
        if (athlete != null) {
            user.setStravaAthleteId(((Number) athlete.get("id")).longValue());
        }
        userRepository.save(user);

        // Sync recent activities in background
        try {
            syncActivities(user);
        } catch (Exception e) {
            // Don't fail the OAuth flow if sync fails
        }

        return frontendUrl + "/devices?strava=connected";
    }

    /** Sync recent Strava activities for user */
    @Transactional
    public int syncActivities(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return syncActivities(user);
    }

    @Transactional
    public int syncActivities(User user) {
        String token = getValidAccessToken(user);
        if (token == null) return 0;

        // Fetch activities from last 90 days
        long after = Instant.now().minusSeconds(90L * 24 * 3600).getEpochSecond();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                STRAVA_ACTIVITIES_URL + "?per_page=50&after=" + after,
                HttpMethod.GET, entity,
                new ParameterizedTypeReference<>() {});

        if (response.getBody() == null) return 0;

        int imported = 0;
        for (Map<String, Object> stravaActivity : response.getBody()) {
            if (saveStravaActivity(user, stravaActivity)) imported++;
        }

        user.setStravaLastSync(Instant.now());
        userRepository.save(user);
        return imported;
    }

    /** Disconnect Strava — clear all tokens */
    @Transactional
    public void disconnect(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setStravaAthleteId(null);
        user.setStravaAccessToken(null);
        user.setStravaRefreshToken(null);
        user.setStravaTokenExpiresAt(null);
        user.setStravaOauthState(null);
        userRepository.save(user);
    }

    /** Handle a webhook event pushed by Strava — fetches only the specific new activity */
    @Transactional
    public void handleWebhookEvent(Map<String, Object> event) {
        String objectType = (String) event.get("object_type");
        String aspectType = (String) event.get("aspect_type");
        if (!"activity".equals(objectType) || !"create".equals(aspectType)) return;

        Long stravaAthleteId = ((Number) event.get("owner_id")).longValue();
        Long activityId = ((Number) event.get("object_id")).longValue();

        userRepository.findByStravaAthleteId(stravaAthleteId).ifPresent(user -> {
            String token = getValidAccessToken(user);
            if (token == null) return;

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(token);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                        STRAVA_ACTIVITY_URL + activityId,
                        HttpMethod.GET, entity,
                        new ParameterizedTypeReference<>() {});

                if (response.getBody() != null) {
                    saveStravaActivity(user, response.getBody());
                    user.setStravaLastSync(Instant.now());
                    userRepository.save(user);
                }
            } catch (Exception ignored) {}
        });
    }

    /** Maps and saves a single Strava activity; returns true if newly imported */
    private boolean saveStravaActivity(User user, Map<String, Object> stravaActivity) {
        String externalId = "strava_" + stravaActivity.get("id");
        if (activityRepository.existsByUserIdAndExternalId(user.getId(), externalId)) return false;

        Activity activity = Activity.builder()
                .user(user)
                .externalId(externalId)
                .source(Activity.Source.STRAVA)
                .sportType(mapSportType((String) stravaActivity.get("type")))
                .durationSeconds(getInt(stravaActivity, "moving_time"))
                .distanceMeters(getInt(stravaActivity, "distance"))
                .elevationGain(getInt(stravaActivity, "total_elevation_gain"))
                .calories(getInt(stravaActivity, "calories"))
                .averageHeartRate(getInt(stravaActivity, "average_heartrate"))
                .maxHeartRate(getInt(stravaActivity, "max_heartrate"))
                .averagePace(getDouble(stravaActivity, "average_speed"))
                .routePolyline(extractPolyline(stravaActivity))
                .build();

        activityRepository.save(activity);
        return true;
    }

    public String getFrontendUrl() { return frontendUrl; }

    public boolean isConnected(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getStravaAccessToken() != null)
                .orElse(false);
    }

    public Map<String, Object> getStatus(Long userId) {
        return userRepository.findById(userId).map(u -> {
            Map<String, Object> status = new HashMap<>();
            status.put("connected", u.getStravaAccessToken() != null);
            status.put("lastSync", u.getStravaLastSync() != null ? u.getStravaLastSync().toString() : null);
            return status;
        }).orElse(Map.of("connected", false, "lastSync", null));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private String getValidAccessToken(User user) {
        if (user.getStravaAccessToken() == null) return null;

        // Refresh if expired (or expiring in next 5 minutes)
        long now = Instant.now().getEpochSecond();
        if (user.getStravaTokenExpiresAt() != null && user.getStravaTokenExpiresAt() <= now + 300) {
            return refreshToken(user);
        }
        return user.getStravaAccessToken();
    }

    private String refreshToken(User user) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", user.getStravaRefreshToken());
            params.add("grant_type", "refresh_token");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    STRAVA_TOKEN_URL, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<>() {});

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                user.setStravaAccessToken((String) body.get("access_token"));
                user.setStravaRefreshToken((String) body.get("refresh_token"));
                user.setStravaTokenExpiresAt(((Number) body.get("expires_at")).longValue());
                userRepository.save(user);
                return user.getStravaAccessToken();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Map<String, Object> exchangeCodeForToken(String code) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("code", code);
            params.add("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    STRAVA_TOKEN_URL, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<>() {});

            return response.getBody();
        } catch (Exception e) {
            return null;
        }
    }

    private Activity.SportType mapSportType(String type) {
        if (type == null) return Activity.SportType.OTHER;
        return switch (type.toLowerCase()) {
            case "run", "virtualrun", "trailrun" -> Activity.SportType.RUN;
            case "ride", "virtualride", "mountainbikeride", "ebikeride" -> Activity.SportType.BIKE;
            case "swim" -> Activity.SportType.SWIM;
            case "hike" -> Activity.SportType.HIKE;
            case "walk" -> Activity.SportType.WALK;
            default -> Activity.SportType.OTHER;
        };
    }

    @SuppressWarnings("unchecked")
    private String extractPolyline(Map<String, Object> activity) {
        Object map = activity.get("map");
        if (map instanceof Map) {
            return (String) ((Map<String, Object>) map).get("summary_polyline");
        }
        return null;
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
