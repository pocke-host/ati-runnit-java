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

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/spotify")
@RequiredArgsConstructor
public class SpotifyController {

    private final SpotifyService spotifyService;

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
