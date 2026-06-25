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

    @Value("${spotify.client.id:}")
    private String clientId;

    @Value("${spotify.client.secret:}")
    private String clientSecret;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // In-memory token cache
    private String cachedAccessToken;
    private long tokenExpiryEpochMs = 0;

    public SpotifyService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
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

    /**
     * Parses the Spotify search JSON response into a list of track maps.
     */
    private List<Map<String, Object>> parseTracksFromResponse(String json) {
        List<Map<String, Object>> tracks = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.path("tracks").path("items");

            for (JsonNode item : items) {
                Map<String, Object> track = new HashMap<>();
                track.put("id", item.path("id").asText(null));
                track.put("name", item.path("name").asText(null));

                // First artist name
                JsonNode artists = item.path("artists");
                String artist = (artists.isArray() && artists.size() > 0)
                        ? artists.get(0).path("name").asText(null)
                        : null;
                track.put("artist", artist);

                track.put("albumName", item.path("album").path("name").asText(null));
                track.put("previewUrl", nullIfEmpty(item.path("preview_url").asText(null)));
                track.put("externalUrl", item.path("external_urls").path("spotify").asText(null));
                track.put("durationMs", item.path("duration_ms").asLong(0));

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
