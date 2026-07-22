package com.runnit.api.service;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleCalendarService {

    private final UserRepository userRepository;

    @Value("${google.client.id}")
    private String clientId;

    @Value("${google.client.secret}")
    private String clientSecret;

    @Value("${google.calendar.redirect.uri}")
    private String redirectUri;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String EVENTS_URL = "https://www.googleapis.com/calendar/v3/calendars/primary/events";
    private static final String SCOPE = "https://www.googleapis.com/auth/calendar.events";

    private static final Map<String, String> SPORT_COLOR = Map.of(
            "RUN", "9", "BIKE", "10", "SWIM", "1", "HIKE", "6", "WALK", "5", "STRENGTH", "3"
    );

    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public String buildAuthorizationUrl(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String state = UUID.randomUUID().toString();
        user.setGoogleCalendarOauthState(state);
        userRepository.save(user);

        return "https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=" + SCOPE +
                "&access_type=offline" +
                "&prompt=consent" +
                "&state=" + state;
    }

    @Transactional
    public String handleCallback(String code, String state) {
        User user = userRepository.findByGoogleCalendarOauthState(state)
                .orElseThrow(() -> new BadRequestException("Invalid OAuth state"));

        Map<String, Object> tokenResponse = exchangeCodeForToken(code);
        if (tokenResponse == null || tokenResponse.get("access_token") == null) {
            return frontendUrl + "/calendar/sync?error=token_exchange_failed";
        }

        user.setGoogleCalendarAccessToken((String) tokenResponse.get("access_token"));
        // Google only returns a refresh_token on first consent (or with prompt=consent, every time) —
        // don't overwrite an existing one with null if it's somehow absent.
        if (tokenResponse.get("refresh_token") != null) {
            user.setGoogleCalendarRefreshToken((String) tokenResponse.get("refresh_token"));
        }
        long expiresIn = ((Number) tokenResponse.getOrDefault("expires_in", 3600)).longValue();
        user.setGoogleCalendarTokenExpiresAt(Instant.now().getEpochSecond() + expiresIn);
        user.setGoogleCalendarOauthState(null);
        userRepository.save(user);

        return frontendUrl + "/calendar/sync?googleCalendar=connected";
    }

    @Transactional
    public void disconnect(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setGoogleCalendarAccessToken(null);
        user.setGoogleCalendarRefreshToken(null);
        user.setGoogleCalendarTokenExpiresAt(null);
        user.setGoogleCalendarOauthState(null);
        userRepository.save(user);
    }

    public Map<String, Object> getStatus(Long userId) {
        return userRepository.findById(userId).<Map<String, Object>>map(u -> {
            Map<String, Object> status = new HashMap<>();
            status.put("connected", u.getGoogleCalendarAccessToken() != null);
            return status;
        }).orElse(Map.of("connected", false));
    }

    public String getFrontendUrl() { return frontendUrl; }

    // ─── Calendar event CRUD ──────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> createEvent(Long userId, Map<String, Object> workout) {
        User user = requireUser(userId);
        String token = getValidAccessToken(user);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildEventBody(workout), authHeaders(token));
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                EVENTS_URL, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});
        return response.getBody();
    }

    @Transactional
    public Map<String, Object> updateEvent(Long userId, String googleEventId, Map<String, Object> workout) {
        User user = requireUser(userId);
        String token = getValidAccessToken(user);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(buildEventBody(workout), authHeaders(token));
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                EVENTS_URL + "/" + googleEventId, HttpMethod.PATCH, entity, new ParameterizedTypeReference<>() {});
        return response.getBody();
    }

    @Transactional
    public void deleteEvent(Long userId, String googleEventId) {
        User user = requireUser(userId);
        String token = getValidAccessToken(user);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token));
        try {
            restTemplate.exchange(EVENTS_URL + "/" + googleEventId, HttpMethod.DELETE, entity, Void.class);
        } catch (HttpClientErrorException.Gone | HttpClientErrorException.NotFound ignored) {
            // Already removed on Google's side — treat as success
        }
    }

    public Map<String, Object> listEvents(Long userId, String timeMin, String timeMax) {
        User user = requireUser(userId);
        String token = getValidAccessToken(user);
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(token));

        String url = EVENTS_URL + "?timeMin=" + timeMin + "&timeMax=" + timeMax
                + "&singleEvents=true&orderBy=startTime&maxResults=100";
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, new ParameterizedTypeReference<>() {});
        return response.getBody();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private User requireUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Map<String, Object> buildEventBody(Map<String, Object> workout) {
        String date = (String) workout.get("date");
        String startTime = (String) workout.getOrDefault("startTime", "07:00");
        Number durationMinutesN = (Number) workout.getOrDefault("durationMinutes", 60);
        int durationMinutes = durationMinutesN != null ? durationMinutesN.intValue() : 60;

        ZoneId zone = ZoneId.systemDefault();
        java.time.LocalDateTime start = java.time.LocalDateTime.parse(date + "T" + startTime + ":00");
        java.time.LocalDateTime end = start.plusMinutes(durationMinutes);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        Map<String, Object> body = new HashMap<>();
        body.put("summary", workout.getOrDefault("title", "Runnit Workout"));
        body.put("description", workout.getOrDefault("description", ""));
        body.put("colorId", SPORT_COLOR.getOrDefault(String.valueOf(workout.get("sport")).toUpperCase(), "9"));
        body.put("start", Map.of("dateTime", start.format(fmt), "timeZone", zone.getId()));
        body.put("end", Map.of("dateTime", end.format(fmt), "timeZone", zone.getId()));
        return body;
    }

    private String getValidAccessToken(User user) {
        if (user.getGoogleCalendarAccessToken() == null) {
            throw new BadRequestException("Not connected to Google Calendar");
        }
        long now = Instant.now().getEpochSecond();
        if (user.getGoogleCalendarTokenExpiresAt() != null && user.getGoogleCalendarTokenExpiresAt() <= now + 300) {
            return refreshToken(user);
        }
        return user.getGoogleCalendarAccessToken();
    }

    private String refreshToken(User user) {
        if (user.getGoogleCalendarRefreshToken() == null) {
            throw new BadRequestException("Google Calendar connection expired — please reconnect");
        }
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", clientId);
        params.add("client_secret", clientSecret);
        params.add("refresh_token", user.getGoogleCalendarRefreshToken());
        params.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                TOKEN_URL, HttpMethod.POST, entity, new ParameterizedTypeReference<>() {});

        if (response.getBody() == null) {
            throw new BadRequestException("Failed to refresh Google Calendar token");
        }
        Map<String, Object> body = response.getBody();
        user.setGoogleCalendarAccessToken((String) body.get("access_token"));
        long expiresIn = ((Number) body.getOrDefault("expires_in", 3600)).longValue();
        user.setGoogleCalendarTokenExpiresAt(Instant.now().getEpochSecond() + expiresIn);
        userRepository.save(user);
        return user.getGoogleCalendarAccessToken();
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
            log.error("Google Calendar token exchange failed: {}", e.getMessage(), e);
            return null;
        }
    }
}
