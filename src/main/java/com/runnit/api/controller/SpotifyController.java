package com.runnit.api.controller;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.service.SpotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import java.net.URI;

import java.util.List;
import java.util.Map;

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
    public ResponseEntity<Void> callback(@RequestParam String code, @RequestParam String state) {
        spotifyService.callback(code, state);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(frontendUrl + "/devices?spotify=connected")).build();
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
