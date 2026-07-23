package com.runnit.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.dto.CorosActivityDTO;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CorosService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ObjectMapper objectMapper;

    @Value("${coros.client.id:}")
    private String clientId;

    @Value("${coros.client.secret:}")
    private String clientSecret;

    @Value("${coros.redirect.uri:https://ati-runnit-java.onrender.com/api/integrations/coros/callback}")
    private String redirectUri;

    @Value("${app.frontend.url:https://runnit.live}")
    private String frontendUrl;

    private static final String AUTHORIZE_URL   = "https://open.coros.com/oauth2/authorize";
    private static final String TOKEN_URL        = "https://open.coros.com/oauth2/accesstoken";
    private static final String REFRESH_URL      = "https://open.coros.com/oauth2/refresh_token";
    private static final String ACTIVITY_LIST    = "https://open.coros.com/v2/coros/sport/list";

    // ─── OAuth ────────────────────────────────────────────────────────────────

    @Transactional
    public String buildAuthorizationUrl(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String state = UUID.randomUUID().toString();
        user.setCorosOauthState(state);
        userRepository.save(user);

        return AUTHORIZE_URL
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&state=" + encode(state)
                + "&scope=activity";
    }

    @Transactional
    public String handleCallback(String code, String state) {
        User user = userRepository.findByCorosOauthState(state)
                .orElseThrow(() -> new ResourceNotFoundException("No user found for COROS oauth state"));

        try {
            Map<String, String> tokens = exchangeCode(code);
            user.setCorosAccessToken(tokens.get("access_token"));
            user.setCorosRefreshToken(tokens.get("refresh_token"));
            long expiresIn = Long.parseLong(tokens.getOrDefault("expires_in", "86400"));
            user.setCorosTokenExpiresAt(Instant.now().getEpochSecond() + expiresIn);
            user.setCorosOauthState(null);
            if (tokens.containsKey("open_id")) user.setCorosUserId(tokens.get("open_id"));
            userRepository.save(user);
        } catch (Exception e) {
            log.error("COROS token exchange failed: {}", e.getMessage(), e);
            return frontendUrl + "/devices?error=coros_token_failed";
        }

        try {
            syncActivities(user);
        } catch (Exception e) {
            log.warn("Post-OAuth COROS sync failed for user {}: {}", user.getId(), e.getMessage());
        }

        return frontendUrl + "/devices?coros=connected";
    }

    // ─── Sync ─────────────────────────────────────────────────────────────────

    @Transactional
    public int syncActivities(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return syncActivities(user);
    }

    @Transactional
    public int syncActivities(User user) {
        if (user.getCorosAccessToken() == null) return 0;

        try {
            ensureTokenFresh(user);

            // Fetch up to 50 activities from the last 90 days
            long since = Instant.now().minusSeconds(90L * 24 * 3600).getEpochSecond();
            String urlStr = ACTIVITY_LIST + "?size=50&pageNumber=1&startTime=" + since;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + user.getCorosAccessToken());
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            if (conn.getResponseCode() != 200) {
                log.warn("COROS activity list returned HTTP {}", conn.getResponseCode());
                return 0;
            }

            CorosActivityDTO response = objectMapper.readValue(
                    conn.getInputStream(), CorosActivityDTO.class);

            if (!"0000".equals(response.getResult()) || response.getData() == null) return 0;

            List<CorosActivityDTO.SportData> sports = response.getData().getSportDataList();
            if (sports == null) return 0;

            int imported = 0;
            for (CorosActivityDTO.SportData sport : sports) {
                if (saveCorosActivity(user, sport)) imported++;
            }

            user.setCorosLastSync(Instant.now());
            userRepository.save(user);
            log.info("COROS sync: imported {} activities for user {}", imported, user.getId());
            return imported;

        } catch (Exception e) {
            log.warn("COROS sync failed for user {}: {}", user.getId(), e.getMessage());
            return 0;
        }
    }

    // Called from webhook when COROS pushes a single completed activity
    @Transactional
    public void handleWebhookActivity(String corosUserId, String labelId) {
        userRepository.findByCorosUserId(corosUserId).ifPresent(user -> {
            try {
                ensureTokenFresh(user);
                // Fetch the activity detail for this specific labelId
                String urlStr = "https://open.coros.com/v2/coros/sport/detail?labelId=" + encode(labelId);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + user.getCorosAccessToken());
                conn.connect();

                if (conn.getResponseCode() == 200) {
                    CorosActivityDTO response = objectMapper.readValue(conn.getInputStream(), CorosActivityDTO.class);
                    if ("0000".equals(response.getResult()) && response.getData() != null) {
                        List<CorosActivityDTO.SportData> list = response.getData().getSportDataList();
                        if (list != null && !list.isEmpty()) {
                            saveCorosActivity(user, list.get(0));
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("COROS webhook activity save failed for user {}: {}", user.getId(), e.getMessage());
            }
        });
    }

    // ─── Status / disconnect ──────────────────────────────────────────────────

    public Map<String, Object> getStatus(Long userId) {
        return userRepository.findById(userId).map(u -> {
            Map<String, Object> status = new HashMap<>();
            status.put("connected", u.getCorosAccessToken() != null);
            status.put("lastSync", u.getCorosLastSync() != null ? u.getCorosLastSync().toString() : null);
            return status;
        }).orElse(Map.of("connected", false, "lastSync", null));
    }

    @Transactional
    public void disconnect(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setCorosAccessToken(null);
        user.setCorosRefreshToken(null);
        user.setCorosTokenExpiresAt(null);
        user.setCorosOauthState(null);
        user.setCorosUserId(null);
        user.setCorosLastSync(null);
        userRepository.save(user);
    }

    public String getFrontendUrl() { return frontendUrl; }

    // ─── Token helpers ────────────────────────────────────────────────────────

    private void ensureTokenFresh(User user) throws Exception {
        Long expiresAt = user.getCorosTokenExpiresAt();
        if (expiresAt == null || Instant.now().getEpochSecond() < expiresAt - 300) return;

        Map<String, String> tokens = refreshToken(user.getCorosRefreshToken());
        user.setCorosAccessToken(tokens.get("access_token"));
        if (tokens.containsKey("refresh_token")) user.setCorosRefreshToken(tokens.get("refresh_token"));
        long expiresIn = Long.parseLong(tokens.getOrDefault("expires_in", "86400"));
        user.setCorosTokenExpiresAt(Instant.now().getEpochSecond() + expiresIn);
        userRepository.save(user);
    }

    private Map<String, String> exchangeCode(String code) throws Exception {
        String body = "grant_type=authorization_code"
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&redirect_uri=" + encode(redirectUri)
                + "&code=" + encode(code);
        return postForm(TOKEN_URL, body);
    }

    private Map<String, String> refreshToken(String refreshToken) throws Exception {
        String body = "grant_type=refresh_token"
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&refresh_token=" + encode(refreshToken);
        return postForm(REFRESH_URL, body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> postForm(String urlStr, String body) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        if (conn.getResponseCode() != 200) {
            throw new RuntimeException("Token request failed with HTTP " + conn.getResponseCode());
        }
        return objectMapper.readValue(conn.getInputStream(), Map.class);
    }

    // ─── Activity persistence ─────────────────────────────────────────────────

    private boolean saveCorosActivity(User user, CorosActivityDTO.SportData sport) {
        if (sport.getLabelId() == null) return false;
        String externalId = "coros_" + sport.getLabelId();
        if (activityRepository.existsByUserIdAndExternalId(user.getId(), externalId)) return false;

        Activity activity = new Activity();
        activity.setUser(user);
        activity.setExternalId(externalId);
        activity.setSource(Activity.Source.COROS);
        activity.setSportType(mapSportType(sport.getMode()));
        activity.setDurationSeconds(sport.getTotalTime());
        activity.setDistanceMeters(sport.getDistance());
        activity.setElevationGain(sport.getTotalAscent());
        activity.setCalories(sport.getCalorie());
        activity.setAverageHeartRate(sport.getAvgHr());
        activity.setMaxHeartRate(sport.getMaxHr());
        if (sport.getStartTime() != null) {
            activity.setPerformedAt(LocalDateTime.ofInstant(Instant.ofEpochSecond(sport.getStartTime()), ZoneOffset.UTC));
        }
        activityRepository.save(activity);
        return true;
    }

    private Activity.SportType mapSportType(Integer mode) {
        if (mode == null) return Activity.SportType.OTHER;
        return switch (mode) {
            case 100, 101, 102 -> Activity.SportType.RUN;   // Run, Trail, Treadmill
            case 200, 201      -> Activity.SportType.BIKE;  // Outdoor, Indoor Cycling
            case 300, 301      -> Activity.SportType.SWIM;  // Open Water, Pool
            case 103           -> Activity.SportType.WALK;
            case 104           -> Activity.SportType.HIKE;
            default            -> Activity.SportType.OTHER;
        };
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
