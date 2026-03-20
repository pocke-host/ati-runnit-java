package com.runnit.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Moments feature.
 * Uses real Tomcat + H2 to catch serialization issues, lazy loading bugs,
 * and business rule enforcement.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MomentTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private String token;
    private String token2;

    // ── Setup ─────────────────────────────────────────────────────────────────

    @BeforeEach
    void setup() {
        token  = registerAndGetToken("moment_user1_" + System.nanoTime() + "@example.com");
        token2 = registerAndGetToken("moment_user2_" + System.nanoTime() + "@example.com");
    }

    // ── Create Moment ─────────────────────────────────────────────────────────

    @Test
    void createMoment_happyPath_returns200WithExpectedFields() {
        ResponseEntity<Map> response = postMoment(token, momentJson("https://s3.example.com/photo.jpg", "Till I Collapse", "Eminem"));

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Create moment should return 200. Body: " + response.getBody());

        Map<?, ?> body = response.getBody();
        assertNotNull(body, "Response body must not be null");
        assertNotNull(body.get("id"), "Response must include id");
        assertNotNull(body.get("user"), "Response must include user info");
        assertNotNull(body.get("photoUrl"), "Response must include photoUrl");
        assertEquals("Till I Collapse", body.get("songTitle"));
        assertEquals("Eminem", body.get("songArtist"));
    }

    @Test
    void createMoment_missingPhotoUrl_returns400() {
        String json = """
                {"songTitle":"Song","songArtist":"Artist"}
                """;
        ResponseEntity<Map> response = postMoment(token, json);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void createMoment_secondMomentSameDay_returns400() {
        // First moment — should succeed
        ResponseEntity<Map> first = postMoment(token, momentJson("https://s3.example.com/1.jpg", "Song A", "Artist A"));
        assertEquals(HttpStatus.OK, first.getStatusCode(), "First moment should succeed");

        // Second moment same day — should be rejected
        ResponseEntity<Map> second = postMoment(token, momentJson("https://s3.example.com/2.jpg", "Song B", "Artist B"));
        assertEquals(HttpStatus.BAD_REQUEST, second.getStatusCode(),
                "Second moment same day should return 400");

        String error = (String) second.getBody().get("error");
        assertNotNull(error);
        assertTrue(error.toLowerCase().contains("already"), "Error should mention already posted. Got: " + error);
    }

    @Test
    void createMoment_unauthenticated_returns403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(momentJson("https://s3.example.com/photo.jpg", "Song", "Artist"), headers);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/moments", req, Map.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    // ── Feed ──────────────────────────────────────────────────────────────────

    @Test
    void getFeed_includesOwnMoments() {
        // Create a moment as user1
        ResponseEntity<Map> created = postMoment(token, momentJson("https://s3.example.com/feed.jpg", "Feed Song", "Feed Artist"));
        assertEquals(HttpStatus.OK, created.getStatusCode());
        Long momentId = ((Number) created.getBody().get("id")).longValue();

        // Fetch own feed
        ResponseEntity<Map> feedResponse = restTemplate.exchange(
                "/api/moments/feed?page=0&size=20",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, feedResponse.getStatusCode(), "Feed should return 200");
        assertNotNull(feedResponse.getBody(), "Feed body must not be null");

        // The feed should be a Page — check content field
        List<?> content = (List<?>) feedResponse.getBody().get("content");
        assertNotNull(content, "Feed must have content array");

        boolean found = content.stream()
                .anyMatch(m -> momentId.equals(((Number) ((Map<?, ?>) m).get("id")).longValue()));
        assertTrue(found, "Own moment should appear in feed");
    }

    @Test
    void getFeed_emptyFeed_returnsOkWithEmptyContent() {
        // New user with no moments and no follows
        String freshToken = registerAndGetToken("moment_empty_" + System.nanoTime() + "@example.com");
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/moments/feed",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(freshToken)),
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<?> content = (List<?>) response.getBody().get("content");
        assertNotNull(content);
    }

    // ── Get Single Moment ──────────────────────────────────────────────────────

    @Test
    void getMoment_existingId_returnsCorrectData() {
        ResponseEntity<Map> created = postMoment(token, momentJson("https://s3.example.com/single.jpg", "Single Song", "Single Artist"));
        Long momentId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/moments/" + momentId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(momentId, ((Number) response.getBody().get("id")).longValue());
        assertNotNull(response.getBody().get("user"));
        assertNotNull(response.getBody().get("reactionCount"));
        assertNotNull(response.getBody().get("commentCount"));
    }

    @Test
    void getMoment_nonExistentId_returns404() {
        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/moments/999999999",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void deleteMoment_ownMoment_returns200() {
        ResponseEntity<Map> created = postMoment(token, momentJson("https://s3.example.com/delete.jpg", "Del Song", "Del Artist"));
        Long momentId = ((Number) created.getBody().get("id")).longValue();

        ResponseEntity<Map> delete = restTemplate.exchange(
                "/api/moments/" + momentId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token)),
                Map.class);

        assertEquals(HttpStatus.OK, delete.getStatusCode());

        // Verify it's gone
        ResponseEntity<Map> get = restTemplate.exchange(
                "/api/moments/" + momentId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)),
                Map.class);
        assertEquals(HttpStatus.NOT_FOUND, get.getStatusCode());
    }

    @Test
    void deleteMoment_otherUsersMoment_returns403() {
        ResponseEntity<Map> created = postMoment(token, momentJson("https://s3.example.com/other.jpg", "Other Song", "Other Artist"));
        Long momentId = ((Number) created.getBody().get("id")).longValue();

        // user2 tries to delete user1's moment
        ResponseEntity<Map> delete = restTemplate.exchange(
                "/api/moments/" + momentId,
                HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(token2)),
                Map.class);

        assertEquals(HttpStatus.FORBIDDEN, delete.getStatusCode());
    }

    // ── Comments ──────────────────────────────────────────────────────────────

    @Test
    void addComment_returnsCommentWithUserInfo() {
        ResponseEntity<Map> created = postMoment(token, momentJson("https://s3.example.com/comment.jpg", "Comment Song", "Comment Artist"));
        Long momentId = ((Number) created.getBody().get("id")).longValue();

        HttpEntity<String> req = new HttpEntity<>("""
                {"text":"Great run!"}
                """, authHeaders(token));
        ResponseEntity<Map> comment = restTemplate.postForEntity("/api/moments/" + momentId + "/comments", req, Map.class);

        assertEquals(HttpStatus.OK, comment.getStatusCode(),
                "Add comment should return 200. Body: " + comment.getBody());
        assertNotNull(comment.getBody().get("id"));
        assertEquals("Great run!", comment.getBody().get("text"));
        assertNotNull(comment.getBody().get("user"));
    }

    @Test
    void getComments_afterAdding_returnsComment() {
        ResponseEntity<Map> created = postMoment(token, momentJson("https://s3.example.com/comments.jpg", "Comments Song", "Comments Artist"));
        Long momentId = ((Number) created.getBody().get("id")).longValue();

        // Add comment
        restTemplate.postForEntity("/api/moments/" + momentId + "/comments",
                new HttpEntity<>("""
                        {"text":"Nice pace!"}
                        """, authHeaders(token2)),
                Map.class);

        // Get comments (public endpoint)
        ResponseEntity<List> response = restTemplate.getForEntity("/api/moments/" + momentId + "/comments", List.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertFalse(response.getBody().isEmpty(), "Comments list should not be empty");
    }

    @Test
    void addComment_emptyText_returns400() {
        ResponseEntity<Map> created = postMoment(token, momentJson("https://s3.example.com/empty.jpg", "Empty Song", "Empty Artist"));
        Long momentId = ((Number) created.getBody().get("id")).longValue();

        HttpEntity<String> req = new HttpEntity<>("""
                {"text":""}
                """, authHeaders(token));
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/moments/" + momentId + "/comments", req, Map.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    // ── Reactions ─────────────────────────────────────────────────────────────

    @Test
    void addReaction_andGetMoment_showsReactionCount() {
        ResponseEntity<Map> created = postMoment(token, momentJson("https://s3.example.com/react.jpg", "React Song", "React Artist"));
        Long momentId = ((Number) created.getBody().get("id")).longValue();

        // Add reaction as user2
        HttpEntity<String> reactReq = new HttpEntity<>("""
                {"type":"FIRE"}
                """, authHeaders(token2));
        ResponseEntity<Map> reaction = restTemplate.postForEntity(
                "/api/moments/" + momentId + "/reaction", reactReq, Map.class);
        assertEquals(HttpStatus.OK, reaction.getStatusCode(), "Add reaction should return 200");

        // Verify reaction count in moment
        ResponseEntity<Map> moment = restTemplate.exchange(
                "/api/moments/" + momentId,
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(token2)),
                Map.class);
        assertEquals(1, ((Number) moment.getBody().get("reactionCount")).intValue());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String momentJson(String photoUrl, String songTitle, String songArtist) {
        return String.format("""
                {"photoUrl":"%s","songTitle":"%s","songArtist":"%s"}
                """, photoUrl, songTitle, songArtist);
    }

    private ResponseEntity<Map> postMoment(String token, String json) {
        return restTemplate.postForEntity("/api/moments",
                new HttpEntity<>(json, authHeaders(token)), Map.class);
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.set("Authorization", "Bearer " + token);
        return h;
    }

    private String registerAndGetToken(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("""
                {"email":"%s","password":"SecurePass1!","displayName":"Test User","role":"athlete"}
                """, email);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/auth/register",
                new HttpEntity<>(body, headers), Map.class);
        return (String) response.getBody().get("token");
    }
}
