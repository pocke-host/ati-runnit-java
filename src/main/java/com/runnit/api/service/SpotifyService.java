package com.runnit.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import com.runnit.api.model.User;
import com.runnit.api.repository.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SpotifyService {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL = "https://api.spotify.com/v1/search";
    private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";
    private static final String RECENTLY_PLAYED_URL = "https://api.spotify.com/v1/me/player/recently-played";
    private static final String USER_SCOPE = "user-read-recently-played";

    @Value("${spotify.client.id:}")
    private String clientId;

    @Value("${spotify.client.secret:}")
    private String clientSecret;

    @Value("${spotify.redirect.uri:https://ati-runnit-java.onrender.com/api/spotify/callback}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;

    // In-memory token cache
    private String cachedAccessToken;
    private long tokenExpiryEpochMs = 0;

    public SpotifyService(RestTemplate restTemplate, ObjectMapper objectMapper, UserRepository userRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
    }

    public String connect(Long userId) {
        requireConfigured();
        User user = userRepository.findById(userId).orElseThrow();
        String state = UUID.randomUUID().toString();
        user.setSpotifyOauthState(state);
        userRepository.save(user);
        return AUTHORIZE_URL + "?response_type=code&client_id=" + enc(clientId)
                + "&scope=" + enc(USER_SCOPE) + "&redirect_uri=" + enc(redirectUri)
                + "&state=" + enc(state);
    }

    public String callback(String code, String state) {
        requireConfigured();
        User user = userRepository.findBySpotifyOauthState(state)
                .orElseThrow(() -> new IllegalStateException("Spotify authorization expired"));
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", code);
        body.add("redirect_uri", redirectUri);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);
        Map<String, Object> token = tokenResponse(new HttpEntity<>(body, headers));
        saveUserToken(user, token);
        user.setSpotifyOauthState(null);
        userRepository.save(user);
        return "";
    }

    public Map<String, Object> status(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return Map.of("connected", user.getSpotifyRefreshToken() != null,
                "expiresAt", user.getSpotifyTokenExpiresAt() == null ? 0 : user.getSpotifyTokenExpiresAt());
    }

    public List<Map<String, Object>> recentlyPlayed(Long userId, Long after, Long before) {
        User user = userRepository.findById(userId).orElseThrow();
        String token = userAccessToken(user);
        StringBuilder url = new StringBuilder(RECENTLY_PLAYED_URL).append("?limit=50");
        if (after != null) url.append("&after=").append(after);
        if (before != null) url.append("&before=").append(before);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        ResponseEntity<String> response = restTemplate.exchange(url.toString(), HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return parseRecentlyPlayed(response.getBody());
    }

    /**
     * Search Spotify tracks by query string.
     * Returns up to 10 track maps, each containing id, name, artist, albumName,
     * previewUrl, externalUrl, and durationMs.
     * Returns an empty list (and logs a warning) if credentials are not configured.
     */
    public List<Map<String, Object>> searchTracks(String query) {
        if (clientId == null || clientId.isBlank()) {
            log.warn("Spotify client ID is not configured — returning empty track list");
            return List.of();
        }

        try {
            String accessToken = getAccessToken();
            String url = SEARCH_URL + "?q=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8)
                    + "&type=track&limit=10";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            return parseTracksFromResponse(response.getBody());

        } catch (Exception e) {
            log.error("Failed to search Spotify tracks for query='{}': {}", query, e.getMessage(), e);
            return List.of();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a valid Spotify access token, refreshing from Spotify if the cached
     * one is expired or missing (client credentials flow).
     */
    private String getAccessToken() {
        long now = System.currentTimeMillis();
        if (cachedAccessToken != null && now < tokenExpiryEpochMs) {
            return cachedAccessToken;
        }

        log.info("Fetching new Spotify access token");

        // Build Basic auth header: Base64(clientId:clientSecret)
        String credentials = clientId + ":" + clientSecret;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + encoded);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_URL, request, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            String token = root.path("access_token").asText();
            int expiresIn = root.path("expires_in").asInt(3600);

            // Cache with a 60-second safety buffer before actual expiry
            cachedAccessToken = token;
            tokenExpiryEpochMs = now + ((expiresIn - 60) * 1000L);

            log.info("Spotify access token obtained, expires in {}s", expiresIn);
            return cachedAccessToken;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Spotify token response: " + e.getMessage(), e);
        }
    }

    private String userAccessToken(User user) {
        if (user.getSpotifyAccessToken() == null) {
            throw new IllegalStateException("Connect Spotify first");
        }
        if (user.getSpotifyTokenExpiresAt() != null && Instant.now().getEpochSecond() < user.getSpotifyTokenExpiresAt() - 60) {
            return user.getSpotifyAccessToken();
        }
        if (user.getSpotifyRefreshToken() == null) throw new IllegalStateException("Spotify authorization expired");
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", user.getSpotifyRefreshToken());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(clientId, clientSecret);
        Map<String, Object> token = tokenResponse(new HttpEntity<>(body, headers));
        saveUserToken(user, token);
        userRepository.save(user);
        return user.getSpotifyAccessToken();
    }

    private Map<String, Object> tokenResponse(HttpEntity<MultiValueMap<String, String>> request) {
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_URL, request, String.class);
            return objectMapper.readValue(response.getBody(), new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Spotify token exchange failed", e);
        }
    }

    private void saveUserToken(User user, Map<String, Object> token) {
        user.setSpotifyAccessToken((String) token.get("access_token"));
        if (token.get("refresh_token") != null) user.setSpotifyRefreshToken((String) token.get("refresh_token"));
        long expiresIn = token.get("expires_in") instanceof Number ? ((Number) token.get("expires_in")).longValue() : 3600;
        user.setSpotifyTokenExpiresAt(Instant.now().getEpochSecond() + expiresIn);
    }

    private List<Map<String, Object>> parseRecentlyPlayed(String json) {
        List<Map<String, Object>> tracks = new ArrayList<>();
        try {
            JsonNode items = objectMapper.readTree(json).path("items");
            for (JsonNode item : items) {
                JsonNode track = item.path("track");
                Map<String, Object> row = parseTrack(track);
                row.put("playedAt", item.path("played_at").asText(null));
                tracks.add(row);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse Spotify listening history", e);
        }
        return tracks;
    }

    private Map<String, Object> parseTrack(JsonNode item) {
        Map<String, Object> track = new HashMap<>();
        track.put("id", item.path("id").asText(null));
        track.put("name", item.path("name").asText(null));
        JsonNode artists = item.path("artists");
        track.put("artist", artists.isArray() && !artists.isEmpty() ? artists.get(0).path("name").asText(null) : null);
        track.put("albumName", item.path("album").path("name").asText(null));
        track.put("externalUrl", item.path("external_urls").path("spotify").asText(null));
        track.put("durationMs", item.path("duration_ms").asLong(0));
        return track;
    }

    private void requireConfigured() {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("Spotify integration is not configured");
        }
    }

    private String enc(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

    /**
     * Parses the Spotify search JSON response into a list of track maps.
     */
    private List<Map<String, Object>> parseTracksFromResponse(String json) {
        List<Map<String, Object>> tracks = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("tracks").path("items");

            for (JsonNode item : items) {
                Map<String, Object> track = parseTrack(item);
                track.put("previewUrl", nullIfEmpty(item.path("preview_url").asText(null)));

                tracks.add(track);
            }
        } catch (Exception e) {
            log.error("Failed to parse Spotify track search response: {}", e.getMessage(), e);
        }
        return tracks;
    }

    private String nullIfEmpty(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
