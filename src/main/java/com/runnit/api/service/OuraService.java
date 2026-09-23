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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OuraService {
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final WellnessDailyRepository wellnessDailyRepository;
    private final AsyncTaskRunner asyncTaskRunner;

    @Value("${oura.client.id}") private String clientId;
    @Value("${oura.client.secret}") private String clientSecret;
    @Value("${oura.redirect.uri}") private String redirectUri;
    @Value("${app.frontend.url}") private String frontendUrl;

    private static final String AUTH_URL = "https://cloud.ouraring.com/oauth/authorize";
    private static final String TOKEN_URL = "https://api.ouraring.com/oauth/token";
    private static final String API_URL = "https://api.ouraring.com/v2/usercollection/";
    private static final String SCOPE = "personal daily workout email";
    private static final int PAGE_SIZE = 100;
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<Long, Object> refreshLocks = new ConcurrentHashMap<>();

    @Transactional
    public String buildAuthorizationUrl(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String state = UUID.randomUUID().toString();
        user.setOuraOauthState(state);
        userRepository.save(user);
        return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
                .queryParam("response_type", "code").queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri).queryParam("scope", SCOPE)
                .queryParam("state", state).build().toUriString();
    }

    @Transactional
    public String handleCallback(String code, String state) {
        User user = userRepository.findByOuraOauthState(state)
                .orElseThrow(() -> new BadRequestException("Invalid OAuth state"));
        Map<String, Object> token = exchange(code, "authorization_code", null);
        if (token == null || token.get("access_token") == null) return frontendUrl + "/devices?error=oura_token_exchange_failed";
        saveTokens(user, token);
        user.setOuraOauthState(null);
        userRepository.save(user);
        Long id = user.getId();
        asyncTaskRunner.run(() -> { try { sync(id); } catch (Exception e) { log.warn("Oura initial sync failed for user {}: {}", id, e.getMessage()); } });
        return frontendUrl + "/devices?oura=connected";
    }

    @Transactional
    public Map<String, Object> getStatus(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (user.getOuraAccessToken() != null) getValidAccessToken(user);
        Map<String, Object> status = new HashMap<>();
        status.put("connected", user.getOuraAccessToken() != null);
        status.put("lastSync", user.getOuraLastSync() == null ? null : user.getOuraLastSync().toString());
        return status;
    }

    @Transactional
    public Map<String, Object> sync(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        int workouts = syncWorkouts(user);
        int wellness = syncWellness(user);
        user.setOuraLastSync(Instant.now());
        userRepository.save(user);
        return Map.of("imported", workouts, "wellnessDays", wellness, "message", workouts + " workouts synced");
    }

    @Transactional
    public void syncAllConnectedUsers() {
        for (User user : userRepository.findByOuraAccessTokenIsNotNull()) {
            try { sync(user.getId()); }
            catch (Exception e) { log.warn("Oura backstop sync failed for user {}: {}", user.getId(), e.getMessage()); }
        }
    }

    @Transactional
    public void disconnect(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setOuraAccessToken(null); user.setOuraRefreshToken(null); user.setOuraTokenExpiresAt(null); user.setOuraOauthState(null);
        userRepository.save(user);
    }

    private int syncWorkouts(User user) {
        String token = getValidAccessToken(user); if (token == null) return 0;
        LocalDate end = LocalDate.now(), start = end.minusDays(90);
        List<Map<String, Object>> records = fetch("workout", token, start, end);
        int imported = 0;
        for (Map<String, Object> workout : records) if (saveWorkout(user, workout)) imported++;
        return imported;
    }

    private int syncWellness(User user) {
        String token = getValidAccessToken(user); if (token == null) return 0;
        LocalDate end = LocalDate.now(), start = end.minusDays(30);
        List<Map<String, Object>> readiness = fetch("daily_readiness", token, start, end);
        List<Map<String, Object>> sleeps = fetch("sleep", token, start, end);
        Map<String, Map<String, Object>> sleepByDay = new HashMap<>();
        for (Map<String, Object> sleep : sleeps) { if (sleep.get("day") != null) sleepByDay.put(String.valueOf(sleep.get("day")), sleep); }
        int saved = 0;
        for (Map<String, Object> item : readiness) {
            String day = String.valueOf(item.get("day"));
            if ("null".equals(day)) continue;
            WellnessDaily row = wellnessDailyRepository.findByUserIdAndDate(user.getId(), LocalDate.parse(day)).orElseGet(WellnessDaily::new);
            row.setUserId(user.getId()); row.setDate(LocalDate.parse(day)); row.setSource("OURA"); row.setExternalCycleId(String.valueOf(item.get("id")));
            row.setRecoveryScore(getInt(item, "score"));
            Map<String, Object> contributors = map(item.get("contributors"));
            row.setRestingHeartRate(getInt(contributors, "resting_heart_rate"));
            Map<String, Object> sleep = sleepByDay.get(day);
            if (sleep != null) {
                row.setSleepPerformancePct(getInt(sleep, "score"));
                row.setSleepEfficiencyPct(getDouble(sleep, "efficiency"));
                row.setTotalSleepMinutes(minutes(sleep, "total_sleep_duration"));
                row.setDeepSleepMinutes(minutes(sleep, "deep_sleep_duration"));
                row.setRemSleepMinutes(minutes(sleep, "rem_sleep_duration"));
                row.setLightSleepMinutes(minutes(sleep, "light_sleep_duration"));
                row.setAwakeMinutes(minutes(sleep, "awake_time"));
            }
            wellnessDailyRepository.save(row); saved++;
        }
        return saved;
    }

    private boolean saveWorkout(User user, Map<String, Object> workout) {
        String externalId = "oura_" + workout.get("id");
        if (activityRepository.existsByUserIdAndExternalId(user.getId(), externalId)) return false;
        Instant start = parseInstant(workout.get("start_datetime"));
        Instant end = parseInstant(workout.get("end_datetime"));
        if (start == null || end == null) return false;
        int duration = (int) Math.max(0, Duration.between(start, end).getSeconds());
        Integer distance = getInt(workout, "distance");
        if (distance == null && workout.get("distance") instanceof Number) distance = (int) Math.round(((Number) workout.get("distance")).doubleValue());
        Double speed = duration > 0 && distance != null && distance > 0 ? distance / (double) duration : null;
        String raw = String.valueOf(workout.getOrDefault("activity", "other"));
        Activity activity = Activity.builder().user(user).externalId(externalId).source(Activity.Source.OURA)
                .sportType(mapSport(raw)).durationSeconds(duration).distanceMeters(distance)
                .calories(getInt(workout, "calories")).averageHeartRate(getInt(workout, "average_heart_rate"))
                .maxHeartRate(getInt(workout, "max_heart_rate")).averagePace(speed)
                .performedAt(start.atZone(ZoneId.of(String.valueOf(workout.getOrDefault("timezone", "UTC")))).toLocalDateTime())
                .notes("OURA: " + titleCase(raw)).build();
        activityRepository.save(activity); return true;
    }

    private List<Map<String, Object>> fetch(String collection, String token, LocalDate start, LocalDate end) {
        List<Map<String, Object>> all = new ArrayList<>(); String next = null;
        for (int page = 0; page < 20; page++) {
            UriComponentsBuilder b = UriComponentsBuilder.fromHttpUrl(API_URL + collection)
                    .queryParam("start_date", start).queryParam("end_date", end).queryParam("limit", PAGE_SIZE);
            if (next != null) b.queryParam("next_token", next);
            HttpHeaders h = new HttpHeaders(); h.setBearerAuth(token);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(b.build().toUri(), HttpMethod.GET, new HttpEntity<>(h), new ParameterizedTypeReference<>() {});
            Map<String, Object> body = response.getBody(); if (body == null) break;
            Object data = body.get("data"); if (data instanceof List<?>) for (Object item : (List<?>) data) if (item instanceof Map<?, ?>) all.add((Map<String, Object>) item);
            next = body.get("next_token") == null ? null : String.valueOf(body.get("next_token")); if (next == null || next.isBlank()) break;
        }
        return all;
    }

    private String getValidAccessToken(User user) {
        if (user.getOuraAccessToken() == null) return null;
        if (user.getOuraTokenExpiresAt() == null || Instant.now().getEpochSecond() < user.getOuraTokenExpiresAt() - 60) return user.getOuraAccessToken();
        Object lock = refreshLocks.computeIfAbsent(user.getId(), id -> new Object());
        synchronized (lock) {
            if (user.getOuraTokenExpiresAt() != null && Instant.now().getEpochSecond() < user.getOuraTokenExpiresAt() - 60) return user.getOuraAccessToken();
            if (user.getOuraRefreshToken() == null) return null;
            try {
                Map<String, Object> token = exchange(null, "refresh_token", user.getOuraRefreshToken());
                if (token == null || token.get("access_token") == null) return null;
                saveTokens(user, token); userRepository.save(user); return user.getOuraAccessToken();
            } catch (Exception e) { log.warn("Oura token refresh failed for user {}: {}", user.getId(), e.getMessage()); return null; }
        }
    }

    private Map<String, Object> exchange(String code, String grantType, String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>(); body.add("grant_type", grantType); body.add("client_id", clientId); body.add("client_secret", clientSecret); body.add("redirect_uri", redirectUri);
        if ("authorization_code".equals(grantType)) body.add("code", code); else body.add("refresh_token", refreshToken);
        try { HttpHeaders h = new HttpHeaders(); h.setContentType(MediaType.APPLICATION_FORM_URLENCODED); return restTemplate.exchange(TOKEN_URL, HttpMethod.POST, new HttpEntity<>(body, h), new ParameterizedTypeReference<Map<String, Object>>() {}).getBody(); }
        catch (HttpClientErrorException e) { log.warn("Oura token exchange failed: {}", e.getResponseBodyAsString()); return null; }
    }

    private void saveTokens(User user, Map<String, Object> token) { user.setOuraAccessToken(String.valueOf(token.get("access_token"))); if (token.get("refresh_token") != null) user.setOuraRefreshToken(String.valueOf(token.get("refresh_token"))); user.setOuraTokenExpiresAt(Instant.now().getEpochSecond() + ((Number) token.getOrDefault("expires_in", 2592000)).longValue()); }
    private Activity.SportType mapSport(String raw) { String s = raw.toLowerCase(Locale.ROOT); if (s.contains("run")) return Activity.SportType.RUN; if (s.contains("cycl") || s.contains("bike")) return Activity.SportType.BIKE; if (s.contains("swim")) return Activity.SportType.SWIM; if (s.contains("walk")) return Activity.SportType.WALK; if (s.contains("hike")) return Activity.SportType.HIKE; return Activity.SportType.OTHER; }
    private Instant parseInstant(Object value) { try { return value == null ? null : Instant.parse(String.valueOf(value)); } catch (Exception e) { return null; } }
    private int minutes(Map<String, Object> map, String key) { Integer seconds = getInt(map, key); return seconds == null ? 0 : seconds / 60; }
    private String titleCase(String value) { return Arrays.stream(value.replace('_', ' ').split(" ")).filter(s -> !s.isBlank()).map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT)).reduce((a, b) -> a + " " + b).orElse(value); }
    @SuppressWarnings("unchecked") private Map<String, Object> map(Object value) { return value instanceof Map<?, ?> ? (Map<String, Object>) value : Map.of(); }
    private Integer getInt(Map<String, Object> map, String key) { Object value = map.get(key); return value instanceof Number ? ((Number) value).intValue() : null; }
    private Double getDouble(Map<String, Object> map, String key) { Object value = map.get(key); return value instanceof Number ? ((Number) value).doubleValue() : null; }
    public String getFrontendUrl() { return frontendUrl; }
}
