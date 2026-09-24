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
import com.runnit.api.repository.ActivityRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Comparator;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SpotifyService {

    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String SEARCH_URL = "https://api.spotify.com/v1/search";
    private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";
    private static final String RECENTLY_PLAYED_URL = "https://api.spotify.com/v1/me/player/recently-played";
    private static final String USER_SCOPE = String.join(" ",
            "user-read-recently-played",
            "user-read-currently-playing",
            "user-read-playback-state",
            "user-modify-playback-state",
            "playlist-read-private",
            "playlist-read-collaborative",
            "playlist-modify-public",
            "playlist-modify-private");

    @Value("${spotify.client.id:}")
    private String clientId;

    @Value("${spotify.client.secret:}")
    private String clientSecret;

    @Value("${spotify.redirect.uri:https://ati-runnit-java.onrender.com/api/spotify/callback}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    // In-memory token cache
    private String cachedAccessToken;
    private long tokenExpiryEpochMs = 0;

    public SpotifyService(RestTemplate restTemplate, ObjectMapper objectMapper, UserRepository userRepository, ActivityRepository activityRepository) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.activityRepository = activityRepository;
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

    public JsonNode currentlyPlaying(Long userId) {
        return userGet(userId, "/v1/me/player?market=US");
    }

    public JsonNode queue(Long userId) {
        return userGet(userId, "/v1/me/player/queue");
    }

    public JsonNode playlists(Long userId) {
        return userGet(userId, "/v1/me/playlists?limit=50");
    }

    /**
     * Summarizes tracks attached to Runnit activities. This deliberately uses
     * activity data instead of Spotify's recently-played endpoint so a summary
     * represents what the athlete listened to during workouts, not unrelated
     * listening from the rest of the day.
     */
    public Map<String, Object> listeningSummary(Long userId, String period) {
        String normalized = "month".equalsIgnoreCase(period) ? "month" : "week";
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = "month".equals(normalized) ? today.withDayOfMonth(1) : today.minusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        var activities = activityRepository.findListeningActivitiesBetween(userId, start, end);

        Map<String, Long> trackCounts = activities.stream().collect(Collectors.groupingBy(
                a -> a.getListeningTrack() + " — " + (a.getListeningArtist() == null ? "Unknown artist" : a.getListeningArtist()),
                LinkedHashMap::new, Collectors.counting()));
        Map<String, Long> artistCounts = activities.stream().collect(Collectors.groupingBy(
                a -> a.getListeningArtist() == null ? "Unknown artist" : a.getListeningArtist(),
                LinkedHashMap::new, Collectors.counting()));

        List<Map<String, Object>> topTracks = trackCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("track", entry.getKey());
                    row.put("plays", entry.getValue());
                    return row;
                }).toList();
        List<Map<String, Object>> topArtists = artistCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("artist", entry.getKey());
                    row.put("plays", entry.getValue());
                    return row;
                })
                .toList();

        long workoutSeconds = activities.stream().mapToLong(a -> a.getDurationSeconds() == null ? 0 : a.getDurationSeconds()).sum();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("period", normalized);
        result.put("startDate", startDate.toString());
        result.put("endDate", today.toString());
        result.put("activitiesWithListening", activities.size());
        result.put("uniqueTracks", trackCounts.size());
        result.put("workoutMinutes", Math.round(workoutSeconds / 60.0));
        result.put("topTracks", topTracks);
        result.put("topArtists", topArtists);
        return result;
    }

    public JsonNode catalog(String type, String id) {
        if (!List.of("artists", "albums", "shows").contains(type)) {
            throw new IllegalArgumentException("Unsupported Spotify catalog type");
        }
        return catalogGet("/v1/" + type + "/" + encPath(id));
    }

    public JsonNode createPlaylist(Long userId, String name, String description, boolean isPublic) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Playlist name is required");
        JsonNode profile = userGet(userId, "/v1/me");
        String spotifyUserId = profile.path("id").asText(null);
        if (spotifyUserId == null || spotifyUserId.isBlank()) throw new IllegalStateException("Spotify profile unavailable");
        Map<String, Object> payload = Map.of(
                "name", name.trim(),
                "public", isPublic,
                "collaborative", false,
                "description", description == null ? "Created from RUNNIT" : description.trim());
        return userPost(userId, "/v1/users/" + encPath(spotifyUserId) + "/playlists", payload);
    }

    /** Creates a private playlist from the user's recent Spotify workout soundtrack. */
    public JsonNode createPlaylistFromHistory(Long userId, String period, String name) {
        long days = "month".equalsIgnoreCase(period) ? 30 : 7;
        long after = Instant.now().minusSeconds(days * 24 * 60 * 60).toEpochMilli();
        List<Map<String, Object>> recent = recentlyPlayed(userId, after, null);
        List<String> uris = recent.stream()
                .map(row -> row.get("id"))
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .filter(id -> !id.isBlank())
                .distinct()
                .map(id -> "spotify:track:" + id)
                .limit(100)
                .toList();
        if (uris.isEmpty()) throw new IllegalStateException("No recent Spotify tracks found");
        JsonNode playlist = createPlaylist(userId,
                name == null || name.isBlank() ? "RUNNIT soundtrack" : name,
                "A RUNNIT workout soundtrack from your recent listening history.", false);
        addTracks(userId, playlist.path("id").asText(), uris);
        return playlist;
    }

    public JsonNode addTracks(Long userId, String playlistId, List<String> uris) {
        if (playlistId == null || playlistId.isBlank() || uris == null || uris.isEmpty()) {
            throw new IllegalArgumentException("Playlist and at least one track are required");
        }
        return userPost(userId, "/v1/playlists/" + encPath(playlistId) + "/tracks", Map.of("uris", uris));
    }

    public void playback(Long userId, String action, Map<String, Object> body) {
        String path = switch (action) {
            case "pause" -> "/v1/me/player/pause";
            case "resume" -> "/v1/me/player/play";
            case "next" -> "/v1/me/player/next";
            case "previous" -> "/v1/me/player/previous";
            case "seek" -> "/v1/me/player/seek";
            default -> throw new IllegalArgumentException("Unsupported playback action");
        };
        userPut(userId, path, body == null ? Map.of() : body);
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
                    + "&type=track,episode&limit=10";

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

    private JsonNode userGet(Long userId, String path) {
        User user = userRepository.findById(userId).orElseThrow();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userAccessToken(user));
        ResponseEntity<String> response = restTemplate.exchange("https://api.spotify.com" + path,
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return parseJson(response.getBody());
    }

    private JsonNode catalogGet(String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(getAccessToken());
        ResponseEntity<String> response = restTemplate.exchange("https://api.spotify.com" + path,
                HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return parseJson(response.getBody());
    }

    private JsonNode userPost(Long userId, String path, Map<String, Object> payload) {
        User user = userRepository.findById(userId).orElseThrow();
        HttpHeaders headers = jsonHeaders(userAccessToken(user));
        ResponseEntity<String> response = restTemplate.exchange("https://api.spotify.com" + path,
                HttpMethod.POST, new HttpEntity<>(payload, headers), String.class);
        return parseJson(response.getBody());
    }

    private void userPut(Long userId, String path, Map<String, Object> payload) {
        User user = userRepository.findById(userId).orElseThrow();
        HttpHeaders headers = jsonHeaders(userAccessToken(user));
        restTemplate.exchange("https://api.spotify.com" + path, HttpMethod.PUT,
                new HttpEntity<>(payload, headers), String.class);
    }

    private HttpHeaders jsonHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private JsonNode parseJson(String body) {
        try { return objectMapper.readTree(body == null || body.isBlank() ? "{}" : body); }
        catch (Exception e) { throw new IllegalStateException("Could not parse Spotify response", e); }
    }

    private String encPath(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }

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
        String artist = artists.isArray() && !artists.isEmpty() ? artists.get(0).path("name").asText(null) : null;
        if (artist == null || artist.isBlank()) artist = item.path("show").path("name").asText(null);
        track.put("artist", artist);
        String albumName = item.path("album").path("name").asText(null);
        if (albumName == null || albumName.isBlank()) albumName = item.path("show").path("name").asText(null);
        track.put("albumName", albumName);
        JsonNode images = item.path("album").path("images");
        if (!images.isArray() || images.isEmpty()) images = item.path("show").path("images");
        track.put("imageUrl", images.isArray() && !images.isEmpty() ? images.get(0).path("url").asText(null) : null);
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
            for (JsonNode item : root.path("episodes").path("items")) {
                Map<String, Object> episode = parseTrack(item);
                episode.put("contentType", "episode");
                episode.put("previewUrl", nullIfEmpty(item.path("audio_preview_url").asText(null)));
                tracks.add(episode);
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
