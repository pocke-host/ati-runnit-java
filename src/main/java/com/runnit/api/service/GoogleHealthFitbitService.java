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

import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Fitbit integration through the Google Health API v4, not the legacy Fitbit API. */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleHealthFitbitService {
    private final UserRepository users;
    private final ActivityRepository activities;
    private final WellnessDailyRepository wellness;
    private final AsyncTaskRunner asyncTaskRunner;

    @Value("${fitbit.google.client.id}") private String clientId;
    @Value("${fitbit.google.client.secret}") private String clientSecret;
    @Value("${fitbit.google.redirect.uri}") private String redirectUri;
    @Value("${app.frontend.url}") private String frontendUrl;

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String HEALTH_URL = "https://health.googleapis.com/v4";
    private static final String SCOPES = String.join(" ",
            "https://www.googleapis.com/auth/googlehealth.activity_and_fitness.readonly",
            "https://www.googleapis.com/auth/googlehealth.sleep.readonly",
            "https://www.googleapis.com/auth/googlehealth.health_metrics_and_measurements.readonly",
            "https://www.googleapis.com/auth/googlehealth.profile.readonly");
    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<Long, Object> refreshLocks = new ConcurrentHashMap<>();

    @Transactional
    public String buildAuthorizationUrl(Long userId) {
        User user = user(userId);
        String state = UUID.randomUUID().toString();
        user.setFitbitGoogleOauthState(state);
        users.save(user);
        return UriComponentsBuilder.fromHttpUrl(AUTH_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", SCOPES)
                .queryParam("access_type", "offline")
                .queryParam("include_granted_scopes", "true")
                .queryParam("prompt", "consent")
                .queryParam("state", state)
                .build().encode().toUriString();
    }

    @Transactional
    public String handleCallback(String code, String state) {
        User user = users.findByFitbitGoogleOauthState(state)
                .orElseThrow(() -> new BadRequestException("Invalid Fitbit OAuth state"));
        Map<String, Object> token = exchange(code, "authorization_code", null);
        if (token == null || token.get("access_token") == null) {
            return frontendUrl + "/devices?error=fitbit_token_exchange_failed";
        }
        saveTokens(user, token);
        Map<String, Object> identity = get("/users/me/identity", user, false);
        user.setFitbitGoogleHealthUserId(string(identity, "healthUserId"));
        user.setFitbitGoogleLegacyUserId(string(identity, "legacyUserId"));
        user.setFitbitGoogleOauthState(null);
        users.save(user);
        Long id = user.getId();
        asyncTaskRunner.run(() -> { try { sync(id); } catch (Exception e) { log.warn("Fitbit initial sync failed for user {}: {}", id, e.getMessage()); } });
        return frontendUrl + "/devices?fitbit=connected";
    }

    @Transactional
    public Map<String, Object> status(Long userId) {
        User user = user(userId);
        boolean connected = user.getFitbitGoogleAccessToken() != null;
        if (connected && getValidAccessToken(user) == null) connected = false;
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("connected", connected);
        status.put("lastSync", user.getFitbitGoogleLastSync() == null ? null : user.getFitbitGoogleLastSync().toString());
        status.put("healthUserId", user.getFitbitGoogleHealthUserId());
        status.put("legacyUserId", user.getFitbitGoogleLegacyUserId());
        return status;
    }

    @Transactional
    public Map<String, Object> sync(Long userId) {
        User user = user(userId);
        int imported = syncExercises(user);
        int sleepDays = syncSleep(user);
        user.setFitbitGoogleLastSync(Instant.now());
        users.save(user);
        return Map.of("imported", imported, "wellnessDays", sleepDays, "message", imported + " Fitbit workouts synced");
    }

    @Transactional
    public void syncAllConnectedUsers() {
        for (User user : users.findByFitbitGoogleAccessTokenIsNotNull()) {
            try { sync(user.getId()); }
            catch (Exception e) { log.warn("Fitbit backstop sync failed for user {}: {}", user.getId(), e.getMessage()); }
        }
    }

    @Transactional
    public void disconnect(Long userId) {
        User user = user(userId);
        user.setFitbitGoogleAccessToken(null); user.setFitbitGoogleRefreshToken(null);
        user.setFitbitGoogleTokenExpiresAt(null); user.setFitbitGoogleOauthState(null);
        user.setFitbitGoogleHealthUserId(null); user.setFitbitGoogleLegacyUserId(null);
        users.save(user);
    }

    private int syncExercises(User user) {
        if (getValidAccessToken(user) == null) return 0;
        LocalDate end = LocalDate.now(), start = end.minusDays(90);
        List<Map<String, Object>> points = list(user, "exercise", "exercise.interval.civil_start_time >= \"" + start + "\" AND exercise.interval.civil_start_time < \"" + end.plusDays(1) + "\"");
        int imported = 0;
        for (Map<String, Object> point : points) if (saveExercise(user, point)) imported++;
        return imported;
    }

    private int syncSleep(User user) {
        if (getValidAccessToken(user) == null) return 0;
        LocalDate end = LocalDate.now(), start = end.minusDays(30);
        List<Map<String, Object>> points = list(user, "sleep", "sleep.interval.civil_end_time >= \"" + start + "\" AND sleep.interval.civil_end_time < \"" + end.plusDays(1) + "\"");
        int saved = 0;
        for (Map<String, Object> point : points) {
            Map<String, Object> sleep = map(point.get("sleep"));
            Map<String, Object> interval = map(sleep.get("interval"));
            Instant begin = instant(interval.get("startTime")), finish = instant(interval.get("endTime"));
            if (begin == null || finish == null || !finish.isAfter(begin)) continue;
            LocalDate date = finish.atZone(ZoneOffset.UTC).toLocalDate();
            WellnessDaily row = wellness.findByUserIdAndDate(user.getId(), date).orElseGet(WellnessDaily::new);
            row.setUserId(user.getId()); row.setDate(date); row.setSource("FITBIT");
            row.setExternalCycleId("fitbit_sleep_" + date);
            row.setTotalSleepMinutes((int) Duration.between(begin, finish).toMinutes());
            wellness.save(row); saved++;
        }
        return saved;
    }

    private boolean saveExercise(User user, Map<String, Object> point) {
        Map<String, Object> exercise = map(point.get("exercise"));
        Map<String, Object> interval = map(exercise.get("interval"));
        Instant start = instant(interval.get("startTime")), end = instant(interval.get("endTime"));
        if (start == null || end == null || !end.isAfter(start)) return false;
        String name = string(exercise, "displayName");
        String external = "fitbit_" + Integer.toHexString(Objects.hash(start, end, name, exercise.get("exerciseType")));
        if (activities.existsByUserIdAndExternalId(user.getId(), external)) return false;
        int duration = (int) Duration.between(start, end).getSeconds();
        Map<String, Object> metrics = map(exercise.get("metricsSummary"));
        Integer distance = integer(metrics, "distanceMillimiters", "distanceMillimeters");
        Double pace = decimal(metrics, "averagePaceSecondsPerMeter");
        Double speed = pace != null && pace > 0 ? 1d / pace : null;
        Integer calories = integer(metrics, "caloriesKcal");
        Integer heartRate = integer(metrics, "averageHeartRateBeatsPerMinute");
        Activity activity = Activity.builder().user(user).externalId(external).source(Activity.Source.FITBIT)
                .sportType(mapSport(string(exercise, "exerciseType"))).durationSeconds(duration)
                .distanceMeters(distance == null ? null : distance / 1000)
                .calories(calories).averageHeartRate(heartRate).averagePace(speed)
                .performedAt(start.atZone(ZoneOffset.UTC).toLocalDateTime())
                .notes("Fitbit: " + (name == null ? "Workout" : name)).build();
        activities.save(activity); return true;
    }

    private List<Map<String, Object>> list(User user, String dataType, String filter) {
        List<Map<String, Object>> all = new ArrayList<>(); String page = null;
        for (int i = 0; i < 20; i++) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(HEALTH_URL + "/users/me/dataTypes/" + dataType + "/dataPoints")
                    .queryParam("pageSize", 25).queryParam("filter", filter);
            if (page != null) builder.queryParam("pageToken", page);
            Map<String, Object> response = get(builder.build().toUriString(), user);
            Object rows = response.get("dataPoints");
            if (rows instanceof List<?> list) for (Object row : list) if (row instanceof Map<?, ?>) all.add((Map<String, Object>) row);
            page = string(response, "nextPageToken"); if (page == null || page.isBlank()) break;
        }
        return all;
    }

    private Map<String, Object> get(String path, User user, boolean healthPath) {
        return get(HEALTH_URL + path, user);
    }

    private Map<String, Object> get(String url, User user) {
        String token = getValidAccessToken(user); if (token == null) return Map.of();
        HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(token); headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<>() {});
        return response.getBody() == null ? Map.of() : response.getBody();
    }

    private String getValidAccessToken(User user) {
        if (user.getFitbitGoogleAccessToken() == null) return null;
        if (user.getFitbitGoogleTokenExpiresAt() == null || Instant.now().getEpochSecond() < user.getFitbitGoogleTokenExpiresAt() - 60) return user.getFitbitGoogleAccessToken();
        Object lock = refreshLocks.computeIfAbsent(user.getId(), id -> new Object());
        synchronized (lock) {
            if (user.getFitbitGoogleTokenExpiresAt() != null && Instant.now().getEpochSecond() < user.getFitbitGoogleTokenExpiresAt() - 60) return user.getFitbitGoogleAccessToken();
            if (user.getFitbitGoogleRefreshToken() == null) return null;
            Map<String, Object> token = exchange(null, "refresh_token", user.getFitbitGoogleRefreshToken());
            if (token == null || token.get("access_token") == null) return null;
            saveTokens(user, token); users.save(user); return user.getFitbitGoogleAccessToken();
        }
    }

    private Map<String, Object> exchange(String code, String grantType, String refreshToken) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>(); body.add("client_id", clientId); body.add("client_secret", clientSecret); body.add("grant_type", grantType);
        if ("authorization_code".equals(grantType)) { body.add("code", code); body.add("redirect_uri", redirectUri); } else body.add("refresh_token", refreshToken);
        HttpHeaders headers = new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        try { return restTemplate.exchange(TOKEN_URL, HttpMethod.POST, new HttpEntity<>(body, headers), new ParameterizedTypeReference<Map<String, Object>>() {}).getBody(); }
        catch (Exception e) { log.warn("Google Health token exchange failed: {}", e.getMessage()); return null; }
    }

    private void saveTokens(User user, Map<String, Object> token) {
        user.setFitbitGoogleAccessToken(string(token, "access_token"));
        if (token.get("refresh_token") != null) user.setFitbitGoogleRefreshToken(string(token, "refresh_token"));
        user.setFitbitGoogleTokenExpiresAt(Instant.now().getEpochSecond() + ((Number) token.getOrDefault("expires_in", 3600)).longValue());
    }

    private User user(Long id) { return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found")); }
    @SuppressWarnings("unchecked") private Map<String, Object> map(Object v) { return v instanceof Map<?, ?> ? (Map<String, Object>) v : Map.of(); }
    private String string(Map<String, Object> map, String key) { return map.get(key) == null ? null : String.valueOf(map.get(key)); }
    private Integer integer(Map<String, Object> map, String... keys) { for (String key : keys) if (map.get(key) instanceof Number n) return n.intValue(); return null; }
    private Double decimal(Map<String, Object> map, String key) { return map.get(key) instanceof Number n ? n.doubleValue() : null; }
    private Instant instant(Object value) { try { return value == null ? null : Instant.parse(String.valueOf(value)); } catch (Exception ignored) { return null; } }
    private Activity.SportType mapSport(String raw) { String s = raw == null ? "" : raw.toLowerCase(Locale.ROOT); if (s.contains("run")) return Activity.SportType.RUN; if (s.contains("bike") || s.contains("cycl")) return Activity.SportType.BIKE; if (s.contains("swim")) return Activity.SportType.SWIM; if (s.contains("walk")) return Activity.SportType.WALK; if (s.contains("hike")) return Activity.SportType.HIKE; if (s.contains("strength") || s.contains("weight")) return Activity.SportType.STRENGTH; return Activity.SportType.OTHER; }
    public String getFrontendUrl() { return frontendUrl; }
}
