package com.runnit.api.service;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.model.Activity;
import com.runnit.api.model.Notification;
import com.runnit.api.model.User;
import com.runnit.api.model.WellnessDaily;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.NotificationRepository;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
    private final NotificationRepository notificationRepository;

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
    private static final String PROFILE_URL = "https://api.prod.whoop.com/developer/v2/user/profile/basic";
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

        // Needed so incoming webhook events (keyed by WHOOP's own user_id) can be
        // mapped back to a Runnit user — without this the webhook has no way to
        // find who a workout.updated/sleep.updated event belongs to.
        try {
            fetchAndStoreWhoopUserId(user);
        } catch (Exception e) {
            log.warn("Failed to fetch WHOOP profile user_id for user {}: {}", user.getId(), e.getMessage());
        }

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

    /**
     * Deletes this user's existing WHOOP-sourced activities and re-syncs from scratch.
     * Needed for anyone who synced before performedAt existed — those rows are all
     * stamped with whatever moment the original sync ran, and re-running /sync alone
     * won't fix them since existing externalIds are skipped as already-imported.
     */
    @Transactional
    public Map<String, Object> resyncActivities(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        long deleted = activityRepository.deleteByUserIdAndSource(user.getId(), Activity.Source.WHOOP);
        int imported = syncActivities(user);
        Map<String, Object> result = new HashMap<>();
        result.put("deleted", deleted);
        result.put("imported", imported);
        return result;
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

    @SuppressWarnings("unchecked")
    private void fetchAndStoreWhoopUserId(User user) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(user.getWhoopAccessToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                PROFILE_URL, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        Map<String, Object> body = response.getBody();
        if (body == null || body.get("user_id") == null) return;

        Long whoopUserId = ((Number) body.get("user_id")).longValue();
        user.setWhoopUserId(whoopUserId);
        userRepository.save(user);
    }

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

        String whoopSportName = (String) workout.get("sport_name");
        Activity.SportType mappedType = mapSportType(whoopSportName);

        Activity activity = Activity.builder()
                .user(user)
                .externalId(externalId)
                .source(Activity.Source.WHOOP)
                .sportType(mappedType)
                .durationSeconds(durationSeconds)
                .distanceMeters(getInt(score, "distance_meter"))
                .calories(calories)
                .averageHeartRate(getInt(score, "average_heart_rate"))
                .maxHeartRate(getInt(score, "max_heart_rate"))
                .elevationGain(getInt(score, "altitude_gain_meter"))
                .performedAt(start.toLocalDateTime())
                // sport_type is a fixed DB enum (RUN/BIKE/SWIM/HIKE/WALK/OTHER) — WHOOP supports ~100
                // named activities, most of which correctly fall to OTHER (no STRENGTH/YOGA/etc. category
                // exists yet). Without this, the specific WHOOP activity name is silently discarded the
                // moment it's bucketed as OTHER — always preserve it so nothing's actually lost.
                .notes(whoopSportName != null ? "WHOOP: " + titleCase(whoopSportName) : null)
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
                    long awakeMs = getLong(stageSummary, "total_awake_time_milli");
                    row.setTotalSleepMinutes((int) ((lightMs + swsMs + remMs) / 60000));
                    row.setLightSleepMinutes((int) (lightMs / 60000));
                    row.setDeepSleepMinutes((int) (swsMs / 60000));
                    row.setRemSleepMinutes((int) (remMs / 60000));
                    row.setAwakeMinutes((int) (awakeMs / 60000));
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
        if (s.contains("jog"))                 return Activity.SportType.RUN;
        return Activity.SportType.OTHER;
    }

    private String titleCase(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] words = s.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase());
        }
        return sb.toString();
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
        } catch (HttpClientErrorException e) {
            // 4xx from WHOOP's token endpoint (e.g. invalid_grant) means the refresh token
            // itself is dead — WHOOP won't accept it again on a later retry. Clearing the
            // tokens here (rather than leaving them in place, as before) makes getStatus()
            // correctly report connected=false instead of silently claiming the connection
            // is still good while every sync quietly no-ops. A one-time reconnect
            // notification beats the user finding out days later that nothing's been syncing.
            log.warn("WHOOP token refresh rejected for user {} (needs reconnect): {}", user.getId(), e.getMessage());
            markNeedsReconnect(user);
        } catch (Exception e) {
            // Network blip / WHOOP 5xx — transient, don't destroy working tokens over it.
            // getValidAccessToken() will just retry on the next sync attempt.
            log.warn("WHOOP token refresh failed for user {} (will retry): {}", user.getId(), e.getMessage());
        }
        return null;
    }

    private void markNeedsReconnect(User user) {
        user.setWhoopAccessToken(null);
        user.setWhoopRefreshToken(null);
        user.setWhoopTokenExpiresAt(null);
        userRepository.save(user);

        notificationRepository.save(Notification.builder()
                .user(user)
                .type("WHOOP_NEEDS_RECONNECT")
                .message("Your WHOOP connection expired. Reconnect it in Devices to keep syncing recovery and workout data.")
                .actor(null)
                .referenceType("WHOOP")
                .build());
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

    // ─── Recurring backstop sync ───────────────────────────────────────────────

    /**
     * Backstop for every connected user, run on a schedule. Two jobs at once:
     * catches anything a missed/failed webhook delivery would otherwise lose,
     * and — just as important — exercises the refresh token regularly so it
     * never goes stale purely from disuse (the original "silent disconnect"
     * risk this whole feature exists to close). Each user is isolated so one
     * failure can't block the rest of the batch.
     */
    public void syncAllConnectedUsers() {
        List<User> connected = userRepository.findByWhoopAccessTokenIsNotNull();
        int synced = 0;
        for (User user : connected) {
            try {
                syncActivities(user);
                syncWellness(user);
                synced++;
            } catch (Exception e) {
                log.warn("WHOOP backstop sync failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
        log.info("WHOOP backstop sync: {}/{} connected users synced", synced, connected.size());
    }

    // ─── Webhooks ────────────────────────────────────────────────────────────

    /**
     * Verifies WHOOP's webhook signature: base64Encode(HMACSHA256(timestamp + rawBody, client_secret)),
     * per https://developer.whoop.com/docs/developing/webhooks/. Constant-time compare to avoid
     * leaking timing information about how much of the signature matched.
     */
    public boolean verifyWebhookSignature(String timestamp, String rawBody, String signatureHeader) {
        if (timestamp == null || rawBody == null || signatureHeader == null) return false;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clientSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal((timestamp + rawBody).getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(computed);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), signatureHeader.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.warn("WHOOP webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Dispatches a verified webhook event. Runs async since WHOOP expects a 2XX
     * within ~1s and a full sync can take longer — the controller acks immediately,
     * this does the actual work. Not a targeted single-record fetch (WHOOP's payload
     * only gives us an id, not the full resource) — reactively re-running the existing
     * incremental sync is simpler and still correct, since already-imported records are
     * skipped by externalId; it's just not bandwidth-optimal. .deleted events aren't
     * handled (no delete path exists in our sync today) — logged and skipped.
     */
    @Async
    public void handleWebhookEvent(Long whoopUserId, String eventType) {
        if (eventType != null && eventType.endsWith(".deleted")) {
            log.info("WHOOP webhook: {} events aren't synced (no delete path yet) — skipping", eventType);
            return;
        }

        Optional<User> userOpt = userRepository.findByWhoopUserId(whoopUserId);
        if (userOpt.isEmpty()) {
            log.warn("WHOOP webhook: no user found for whoop user_id {}", whoopUserId);
            return;
        }
        User user = userOpt.get();

        try {
            if (eventType != null && eventType.startsWith("workout.")) {
                syncActivities(user);
            } else if (eventType != null && (eventType.startsWith("sleep.") || eventType.startsWith("recovery."))) {
                syncWellness(user);
            }
        } catch (Exception e) {
            log.warn("WHOOP webhook: sync failed for user {} event {}: {}", user.getId(), eventType, e.getMessage());
        }
    }
}
