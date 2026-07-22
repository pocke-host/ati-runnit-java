package com.runnit.api.service;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.model.WellnessDaily;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.repository.WellnessDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhoopService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final WellnessDailyRepository wellnessDailyRepository;

    @Value("${whoop.client.id}")
    private String clientId;

    @Value("${whoop.client.secret}")
    private String clientSecret;

    @Value("${whoop.redirect.uri}")
    private String redirectUri;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String AUTH_URL = "https://api.prod.whoop.com/oauth/oauth2/auth";
    private static final String TOKEN_URL = "https://api.prod.whoop.com/oauth/oauth2/token";
    private static final String WORKOUT_URL = "https://api.prod.whoop.com/developer/v2/activity/workout";
    private static final String SLEEP_URL = "https://api.prod.whoop.com/developer/v2/activity/sleep";
    private static final String RECOVERY_URL = "https://api.prod.whoop.com/developer/v2/recovery";
    private static final String CYCLE_URL = "https://api.prod.whoop.com/developer/v2/cycle";
    private static final String SCOPE = "read:workout read:sleep read:recovery read:cycles read:profile read:body_measurement offline";

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public String buildAuthorizationUrl(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String state = UUID.randomUUID().toString();
        user.setWhoopOauthState(state);
        userRepository.save(user);

        return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPE)
                .queryParam("state", state)
                .build().toUriString();
    }

    @Transactional
    public String handleCallback(String code, String state) {
        User user = userRepository.findByWhoopOauthState(state)
                .orElseThrow(() -> new BadRequestException("Invalid OAuth state"));

        Map<String, Object> tokenResponse = exchangeCodeForToken(code);
        if (tokenResponse == null || tokenResponse.get("access_token") == null) {
            return frontendUrl + "/devices?error=whoop_token_exchange_failed";
        }

        user.setWhoopAccessToken((String) tokenResponse.get("access_token"));
        if (tokenResponse.get("refresh_token") != null) {
            user.setWhoopRefreshToken((String) tokenResponse.get("refresh_token"));
        }
        long expiresIn = ((Number) tokenResponse.getOrDefault("expires_in", 3600)).longValue();
        user.setWhoopTokenExpiresAt(Instant.now().getEpochSecond() + expiresIn);
        user.setWhoopOauthState(null);
        userRepository.save(user);

        try {
            syncActivities(user);
        } catch (Exception e) {
            log.warn("Post-OAuth WHOOP activity sync failed for user {}: {}", user.getId(), e.getMessage());
        }
        try {
            syncWellness(user);
        } catch (Exception e) {
            log.warn("Post-OAuth WHOOP wellness sync failed for user {}: {}", user.getId(), e.getMessage());
        }

        return frontendUrl + "/devices?whoop=connected";
    }

    @Transactional
    public int syncActivities(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return syncActivities(user);
    }

    private static final int MAX_SYNC_PAGES = 20; // 20 * 25 = 500 records per sync — safety cap, not an expected ceiling

    @Transactional
    public int syncActivities(User user) {
        String token = getValidAccessToken(user);
        if (token == null) return 0;

        Instant start = Instant.now().minusSeconds(90L * 24 * 3600);
        List<Map<String, Object>> workouts = fetchAllPages(WORKOUT_URL, token, start);

        int imported = 0;
        for (Map<String, Object> workout : workouts) {
            if (saveWhoopWorkout(user, workout)) imported++;
        }

        user.setWhoopLastSync(Instant.now());
        userRepository.save(user);
        return imported;
    }

    @Transactional
    public int syncWellness(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return syncWellness(user);
    }

    @Transactional
    public int syncWellness(User user) {
        String token = getValidAccessToken(user);
        if (token == null) return 0;

        Instant start = Instant.now().minusSeconds(30L * 24 * 3600); // daily-cadence data — 30 days is plenty

        List<Map<String, Object>> cycles = fetchAllPages(CYCLE_URL, token, start);
        List<Map<String, Object>> recoveries = fetchAllPages(RECOVERY_URL, token, start);
        List<Map<String, Object>> sleeps = fetchAllPages(SLEEP_URL, token, start);

        Map<Object, Map<String, Object>> recoveryByCycle = new HashMap<>();
        for (Map<String, Object> r : recoveries) {
            if ("SCORED".equals(r.get("score_state"))) recoveryByCycle.put(r.get("cycle_id"), r);
        }
        Map<Object, Map<String, Object>> sleepByCycle = new HashMap<>();
        for (Map<String, Object> s : sleeps) {
            if ("SCORED".equals(s.get("score_state")) && Boolean.FALSE.equals(s.get("nap"))) {
                sleepByCycle.put(s.get("cycle_id"), s);
            }
        }

        int saved = 0;
        for (Map<String, Object> cycle : cycles) {
            if (!"SCORED".equals(cycle.get("score_state"))) continue;
            if (saveWellnessDay(user, cycle, recoveryByCycle.get(cycle.get("id")), sleepByCycle.get(cycle.get("id")))) saved++;
        }
        return saved;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchAllPages(String baseUrl, String token, Instant start) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<Map<String, Object>> all = new ArrayList<>();
        String nextToken = null;

        for (int page = 0; page < MAX_SYNC_PAGES; page++) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("limit", 25)
                    .queryParam("start", start.toString());
            if (nextToken != null) builder.queryParam("nextToken", nextToken);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    builder.build().toUriString(), HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});

            if (response.getBody() == null) break;

            List<Map<String, Object>> records = (List<Map<String, Object>>) response.getBody().get("records");
            if (records == null || records.isEmpty()) break;
            all.addAll(records);

            nextToken = (String) response.getBody().get("next_token");
            if (nextToken == null) break;
        }
        return all;
    }

    @Transactional
    public void disconnect(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setWhoopAccessToken(null);
        user.setWhoopRefreshToken(null);
        user.setWhoopTokenExpiresAt(null);
        user.setWhoopOauthState(null);
        user.setWhoopUserId(null);
        userRepository.save(user);
    }

    public Map<String, Object> getStatus(Long userId) {
        return userRepository.findById(userId).<Map<String, Object>>map(u -> {
            Map<String, Object> status = new HashMap<>();
            status.put("connected", u.getWhoopAccessToken() != null);
            status.put("lastSync", u.getWhoopLastSync() != null ? u.getWhoopLastSync().toString() : null);
            return status;
        }).orElse(Map.of("connected", false, "lastSync", null));
    }

    public String getFrontendUrl() { return frontendUrl; }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private boolean saveWhoopWorkout(User user, Map<String, Object> workout) {
        String externalId = "whoop_" + workout.get("id");
        if (activityRepository.existsByUserIdAndExternalId(user.getId(), externalId)) return false;

        if (!"SCORED".equals(workout.get("score_state"))) return false; // PENDING_SCORE/UNSCORABLE — skip until scored
        Map<String, Object> score = (Map<String, Object>) workout.get("score");
        if (score == null) return false;

        OffsetDateTime start = OffsetDateTime.parse((String) workout.get("start"));
        OffsetDateTime end = OffsetDateTime.parse((String) workout.get("end"));
        int durationSeconds = (int) (end.toEpochSecond() - start.toEpochSecond());

        Double kilojoule = getDouble(score, "kilojoule");
        Integer calories = kilojoule != null ? (int) Math.round(kilojoule / 4.184) : null;

        Activity activity = Activity.builder()
                .user(user)
                .externalId(externalId)
                .source(Activity.Source.WHOOP)
                .sportType(mapSportType((String) workout.get("sport_name")))
                .durationSeconds(durationSeconds)
                .distanceMeters(getInt(score, "distance_meter"))
                .calories(calories)
                .averageHeartRate(getInt(score, "average_heart_rate"))
                .maxHeartRate(getInt(score, "max_heart_rate"))
                .elevationGain(getInt(score, "altitude_gain_meter"))
                .build();

        activityRepository.save(activity);
        return true;
    }

    @SuppressWarnings("unchecked")
    private boolean saveWellnessDay(User user, Map<String, Object> cycle,
                                     Map<String, Object> recovery, Map<String, Object> sleep) {
        Object startRaw = cycle.get("start");
        if (startRaw == null) return false;
        LocalDate date = OffsetDateTime.parse((String) startRaw).toLocalDate();

        WellnessDaily row = wellnessDailyRepository.findByUserIdAndDate(user.getId(), date)
                .orElseGet(WellnessDaily::new);
        row.setUserId(user.getId());
        row.setDate(date);
        row.setSource("WHOOP");
        row.setExternalCycleId(String.valueOf(cycle.get("id")));

        Map<String, Object> cycleScore = (Map<String, Object>) cycle.get("score");
        if (cycleScore != null) row.setStrain(getDouble(cycleScore, "strain"));

        if (recovery != null) {
            Map<String, Object> recScore = (Map<String, Object>) recovery.get("score");
            if (recScore != null) {
                row.setRecoveryScore(getInt(recScore, "recovery_score"));
                row.setHrvMilli(getDouble(recScore, "hrv_rmssd_milli"));
                row.setRestingHeartRate(getInt(recScore, "resting_heart_rate"));
            }
        }

        if (sleep != null) {
            Map<String, Object> sleepScore = (Map<String, Object>) sleep.get("score");
            if (sleepScore != null) {
                row.setSleepPerformancePct(getInt(sleepScore, "sleep_performance_percentage"));
                Double efficiency = getDouble(sleepScore, "sleep_efficiency_percentage");
                row.setSleepEfficiencyPct(efficiency);

                Map<String, Object> stageSummary = (Map<String, Object>) sleepScore.get("stage_summary");
                if (stageSummary != null) {
                    long lightMs = getLong(stageSummary, "total_light_sleep_time_milli");
                    long swsMs = getLong(stageSummary, "total_slow_wave_sleep_time_milli");
                    long remMs = getLong(stageSummary, "total_rem_sleep_time_milli");
                    row.setTotalSleepMinutes((int) ((lightMs + swsMs + remMs) / 60000));
                }
            }
        }

        wellnessDailyRepository.save(row);
        return true;
    }

    private Activity.SportType mapSportType(String sportName) {
        if (sportName == null) return Activity.SportType.OTHER;
        String s = sportName.toLowerCase();
        if (s.contains("run"))                 return Activity.SportType.RUN;
        if (s.contains("cycl") || s.contains("bike")) return Activity.SportType.BIKE;
        if (s.contains("swim"))                return Activity.SportType.SWIM;
        if (s.contains("hik"))                 return Activity.SportType.HIKE;
        if (s.contains("walk"))                return Activity.SportType.WALK;
        return Activity.SportType.OTHER;
    }

    private String getValidAccessToken(User user) {
        if (user.getWhoopAccessToken() == null) return null;

        long now = Instant.now().getEpochSecond();
        if (user.getWhoopTokenExpiresAt() != null && user.getWhoopTokenExpiresAt() <= now + 300) {
            return refreshToken(user);
        }
        return user.getWhoopAccessToken();
    }

    private String refreshToken(User user) {
        if (user.getWhoopRefreshToken() == null) return null;
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("refresh_token", user.getWhoopRefreshToken());
            params.add("grant_type", "refresh_token");
            params.add("scope", SCOPE);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                user.setWhoopAccessToken((String) body.get("access_token"));
                if (body.get("refresh_token") != null) {
                    user.setWhoopRefreshToken((String) body.get("refresh_token"));
                }
                long expiresIn = ((Number) body.getOrDefault("expires_in", 3600)).longValue();
                user.setWhoopTokenExpiresAt(Instant.now().getEpochSecond() + expiresIn);
                userRepository.save(user);
                return user.getWhoopAccessToken();
            }
        } catch (Exception e) {
            log.warn("WHOOP token refresh failed for user {}: {}", user.getId(), e.getMessage());
        }
        return null;
    }

    private Map<String, Object> exchangeCodeForToken(String code) {
        try {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", clientId);
            params.add("client_secret", clientSecret);
            params.add("code", code);
            params.add("redirect_uri", redirectUri);
            params.add("grant_type", "authorization_code");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    TOKEN_URL, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});
            return response.getBody();
        } catch (Exception e) {
            log.error("WHOOP token exchange failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private Integer getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).intValue() : null;
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).doubleValue() : null;
    }

    private long getLong(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }
}
