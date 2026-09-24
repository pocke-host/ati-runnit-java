package com.runnit.api.controller;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.service.SpotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private final SpotifyService spotifyService;
    @Value("${app.frontend.url:https://runnit.live}")
    private String frontendUrl;

    @GetMapping("/connect")
    public Map<String, String> connect(Authentication auth) {
        return Map.of("url", spotifyService.connect((Long) auth.getPrincipal()));
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error) {
        if (StringUtils.hasText(error) || !StringUtils.hasText(code) || !StringUtils.hasText(state)) {
            log.info("Spotify authorization was not completed: {}", error == null ? "missing_code_or_state" : error);
            return redirectToDevices("error", error == null ? "missing_code_or_state" : error);
        }
        try {
            spotifyService.callback(code, state);
            return redirectToDevices("connected", null);
        } catch (Exception e) {
            log.warn("Spotify authorization callback failed: {}", e.getMessage());
            return redirectToDevices("error", "authorization_failed");
        }
    }

    private ResponseEntity<Void> redirectToDevices(String status, String reason) {
        String location = frontendUrl + "/devices?spotify=" + enc(status);
        if (reason != null) location += "&reason=" + enc(reason);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    private String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @PostMapping("/mobile-callback")
    public ResponseEntity<?> mobileCallback(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        String state = body.get("state");
        if (code == null || state == null) return ResponseEntity.badRequest().body(Map.of("error", "code and state are required"));
        try { spotifyService.callback(code, state); return ResponseEntity.ok(Map.of("connected", true)); }
        catch (Exception e) { log.error("Mobile Spotify callback failed", e); return ResponseEntity.badRequest().body(Map.of("error", "Spotify connection failed")); }
    }

    @GetMapping("/status")
    public Map<String, Object> status(Authentication auth) {
        return spotifyService.status((Long) auth.getPrincipal());
    }

    @GetMapping("/recently-played")
    public ResponseEntity<List<Map<String, Object>>> recentlyPlayed(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Long before,
            Authentication auth) {
        return ResponseEntity.ok(spotifyService.recentlyPlayed((Long) auth.getPrincipal(), after, before));
    }

    @GetMapping("/listening-summary")
    public ResponseEntity<?> listeningSummary(
            @RequestParam(defaultValue = "week") String period,
            Authentication auth) {
        if (!"week".equalsIgnoreCase(period) && !"month".equalsIgnoreCase(period)) {
            return ResponseEntity.badRequest().body(Map.of("error", "period must be week or month"));
        }
        return ResponseEntity.ok(spotifyService.listeningSummary((Long) auth.getPrincipal(), period));
    }

    @GetMapping("/currently-playing")
    public ResponseEntity<?> currentlyPlaying(Authentication auth) {
        return ResponseEntity.ok(spotifyService.currentlyPlaying((Long) auth.getPrincipal()));
    }

    @GetMapping("/queue")
    public ResponseEntity<?> queue(Authentication auth) {
        return ResponseEntity.ok(spotifyService.queue((Long) auth.getPrincipal()));
    }

    @GetMapping("/playlists")
    public ResponseEntity<?> playlists(Authentication auth) {
        return ResponseEntity.ok(spotifyService.playlists((Long) auth.getPrincipal()));
    }

    @GetMapping("/{type}/{id}")
    public ResponseEntity<?> catalog(@PathVariable String type, @PathVariable String id) {
        return ResponseEntity.ok(spotifyService.catalog(type, id));
    }

    @PostMapping("/playlists")
    public ResponseEntity<?> createPlaylist(@RequestBody Map<String, Object> body, Authentication auth) {
        String name = (String) body.get("name");
        String description = (String) body.get("description");
        boolean isPublic = Boolean.TRUE.equals(body.get("public"));
        return ResponseEntity.ok(spotifyService.createPlaylist((Long) auth.getPrincipal(), name, description, isPublic));
    }

    @PostMapping("/playlists/{playlistId}/tracks")
    public ResponseEntity<?> addTracks(@PathVariable String playlistId, @RequestBody Map<String, Object> body, Authentication auth) {
        Object rawUris = body.get("uris");
        if (!(rawUris instanceof List<?> list)) return ResponseEntity.badRequest().body(Map.of("error", "uris must be an array"));
        List<String> uris = list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
        return ResponseEntity.ok(spotifyService.addTracks((Long) auth.getPrincipal(), playlistId, uris));
    }

    @PostMapping("/playback/{action}")
    public ResponseEntity<?> playback(@PathVariable String action, @RequestBody(required = false) Map<String, Object> body, Authentication auth) {
        spotifyService.playback((Long) auth.getPrincipal(), action, body);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/spotify/search?q=QUERY
     *
     * Requires authentication. Searches Spotify for tracks matching the query.
     * Returns a JSON array of up to 10 track objects, each containing:
     * id, name, artist, albumName, previewUrl, externalUrl, durationMs.
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchTracks(
            @RequestParam("q") String q,
            Authentication auth) {

        if (!StringUtils.hasText(q)) {
            throw new BadRequestException("Query is required");
        }

        Long userId = (Long) auth.getPrincipal();
        log.debug("Spotify track search requested by userId={} query='{}'", userId, q);

        List<Map<String, Object>> tracks = spotifyService.searchTracks(q);
        return ResponseEntity.ok(tracks);
    }
}
