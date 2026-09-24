package com.runnit.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Service
public class RaceResultDiscoveryService {
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;
    private final RunSignupRequestGate runSignupGate;
    private final RunSignupOAuthService runSignupOAuth;
    @Value("${athlinks.api.key:}") private String athlinksKey;
    @Value("${runsignup.api.key:}") private String runSignupKey;
    @Value("${runsignup.api.secret:}") private String runSignupSecret;
    @Value("${runsignup.api.caller-token:}") private String runSignupCallerToken;
    @Value("${runsignup.api.caller-secret:}") private String runSignupCallerSecret;
    @Value("${raceroster.api.token:}") private String raceRosterToken;

    public RaceResultDiscoveryService(RestTemplate restTemplate, ObjectMapper mapper, RunSignupRequestGate runSignupGate, RunSignupOAuthService runSignupOAuth) { this.restTemplate = restTemplate; this.mapper = mapper; this.runSignupGate = runSignupGate; this.runSignupOAuth = runSignupOAuth; }

    public List<Map<String,Object>> discover(User user, String provider, String raceId, String eventId) {
        if ("ATHLINKS".equalsIgnoreCase(provider)) return athlinks(user);
        if ("RUNSIGNUP".equalsIgnoreCase(provider)) return runSignup(user, raceId, eventId);
        if ("RACEROSTER".equalsIgnoreCase(provider)) return raceRoster(user, raceId, eventId);
        return List.of();
    }

    public List<String> providers() {
        List<String> providers = new ArrayList<>(List.of("ATHLINKS", "RUNSIGNUP"));
        if (raceRosterToken != null && !raceRosterToken.isBlank()) providers.add("RACEROSTER");
        return providers;
    }

    private List<Map<String,Object>> athlinks(User user) {
        if (athlinksKey == null || athlinksKey.isBlank()) throw new IllegalStateException("Athlinks integration is not configured");
        String name = user.getDisplayName();
        if (name == null || name.isBlank()) name = user.getUser();
        String encoded = URLEncoder.encode(name, StandardCharsets.UTF_8);
        String url = "https://api.athlinks.com/results/search/" + encoded + "?key=" + URLEncoder.encode(athlinksKey, StandardCharsets.UTF_8) + "&Includeclaimed=true";
        try {
            JsonNode root = mapper.readTree(restTemplate.getForObject(url, String.class));
            List<Map<String,Object>> results = new ArrayList<>();
            JsonNode rows = root.isArray() ? root : root.path("Results");
            if (!rows.isArray()) rows = root.path("results");
            if (!rows.isArray()) return results;
            for (JsonNode row : rows) {
                Map<String,Object> result = new LinkedHashMap<>();
                String id = first(row, "ResultID", "ResultId", "resultId", "id");
                result.put("provider", "ATHLINKS"); result.put("externalResultId", id);
                result.put("raceName", first(row, "RaceName", "raceName", "EventName", "eventName"));
                result.put("raceDate", first(row, "RaceDate", "raceDate", "EventDate", "eventDate"));
                result.put("distance", first(row, "CourseName", "courseName", "Distance", "distance"));
                result.put("finishTimeSeconds", seconds(row, "FinalTime", "finalTime", "FinishTime", "finishTime"));
                result.put("placement", integer(row, "Place", "place", "OverallPlace", "overallPlace"));
                result.put("resultUrl", first(row, "ResultUrl", "resultUrl", "Url", "url"));
                result.put("matchConfidence", 70);
                if (result.get("raceName") != null) results.add(result);
            }
            return results;
        } catch (Exception e) { log.warn("Athlinks result discovery failed: {}", e.getMessage()); throw new IllegalStateException("Athlinks result search failed", e); }
    }

    private List<Map<String,Object>> runSignup(User user, String raceId, String eventId) {
        if (runSignupKey.isBlank() || runSignupSecret.isBlank()) throw new IllegalStateException("RunSignup integration is not configured");
        if (raceId == null || eventId == null) throw new IllegalArgumentException("RunSignup requires raceId and eventId");
        String[] name = (user.getDisplayName() == null ? "" : user.getDisplayName().trim()).split("\\s+", 2);
        String first = name.length > 0 ? name[0] : "";
        String last = name.length > 1 ? name[1] : "";
        boolean userAuthorized = user.getRunSignupRefreshToken() != null;
        String url = "https://api.runsignup.com/rest/race/" + enc(raceId) + "/results/get-results?format=json&event_id=" + enc(eventId) + "&first_name=" + enc(first) + "&last_name=" + enc(last) + "&results_per_page=100";
        if (!userAuthorized) url += "&api_key=" + enc(runSignupKey) + "&api_secret=" + enc(runSignupSecret) + "&rsu_api_reg=" + enc(runSignupCallerToken);
        final String requestUrl = url;
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (userAuthorized) headers.setBearerAuth(runSignupOAuth.accessToken(user));
            else headers.set("X-RSU-API-REG-SECRET", runSignupCallerSecret);
            String body = runSignupGate.execute(() -> restTemplate.exchange(requestUrl, org.springframework.http.HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers), String.class).getBody());
            JsonNode root = mapper.readTree(body);
            List<JsonNode> rows = new ArrayList<>(); collectResults(root, rows);
            List<Map<String,Object>> out = new ArrayList<>();
            for (JsonNode row : rows) {
                Map<String,Object> result = new LinkedHashMap<>(); result.put("provider","RUNSIGNUP");
                result.put("externalResultId", first(row,"result_id","resultId","registration_id","registrationId"));
                result.put("raceName", first(row,"race_name","raceName","event_name","eventName"));
                result.put("raceDate", first(row,"race_date","raceDate","event_date","eventDate"));
                result.put("distance", first(row,"event_name","eventName","distance"));
                result.put("finishTimeSeconds", seconds(row,"chip_time","chipTime","clock_time","clockTime","finish_time","finishTime"));
                result.put("placement", integer(row,"place","overall_place","overallPlace"));
                result.put("resultUrl", first(row,"result_url","resultUrl","url")); result.put("matchConfidence",85);
                if (result.get("externalResultId") != null) out.add(result);
            }
            return out;
        } catch (Exception e) { log.warn("RunSignup result discovery failed: {}", e.getMessage()); throw new IllegalStateException("RunSignup result search failed", e); }
    }

    /**
     * Race Roster exposes official posted results through its OAuth-protected API.
     * The token is intentionally server-side; it must never be sent to the client.
     * raceId is Race Roster's resultsRaceId and eventId is the event ID.
     */
    private List<Map<String,Object>> raceRoster(User user, String resultsRaceId, String eventId) {
        if (raceRosterToken == null || raceRosterToken.isBlank()) throw new IllegalStateException("Race Roster integration is not configured");
        if (eventId == null || resultsRaceId == null) throw new IllegalArgumentException("Race Roster requires eventId and resultsRaceId");
        String url = "https://raceroster.com/api/v1/events/" + enc(eventId) + "/results?resultsRaceId=" + enc(resultsRaceId) + "&page=1&perPage=100";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(raceRosterToken);
        headers.set("Accept", "application/json");
        try {
            String body = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
            JsonNode root = mapper.readTree(body);
            JsonNode rows = root.path("data");
            if (!rows.isArray()) return List.of();
            String displayName = user.getDisplayName() == null ? user.getUser() : user.getDisplayName();
            List<Map<String,Object>> out = new ArrayList<>();
            for (JsonNode row : rows) {
                if (!containsName(row, displayName)) continue;
                Map<String,Object> result = new LinkedHashMap<>();
                result.put("provider", "RACEROSTER");
                result.put("externalResultId", first(row, "resultSetId", "resultId", "bib", "registrationId"));
                result.put("raceName", first(row, "raceName", "eventName", "name"));
                result.put("raceDate", first(row, "raceDate", "eventDate", "dateCreated"));
                result.put("distance", first(row, "distance", "courseName"));
                result.put("finishTimeSeconds", secondsFromTree(row));
                result.put("placement", firstIntegerFromTree(row));
                result.put("resultUrl", first(row, "resultsUrl", "resultUrl", "url"));
                result.put("matchConfidence", 80);
                out.add(result);
            }
            return out;
        } catch (Exception e) {
            log.warn("Race Roster result discovery failed: {}", e.getMessage());
            throw new IllegalStateException("Race Roster result search failed", e);
        }
    }

    private static boolean containsName(JsonNode node, String name) {
        if (name == null || name.isBlank() || node == null) return false;
        String normalized = name.trim().toLowerCase(Locale.ROOT);
        if (node.isValueNode()) return node.asText("").toLowerCase(Locale.ROOT).contains(normalized);
        if (node.isContainerNode()) {
            for (JsonNode child : node) if (containsName(child, name)) return true;
        }
        return false;
    }

    private static Integer firstIntegerFromTree(JsonNode node) {
        if (node == null) return null;
        if (node.isValueNode()) {
            String value = node.asText("");
            if (value.matches("\\d{1,5}")) try { return Integer.valueOf(value); } catch (Exception ignored) { }
        }
        if (node.isContainerNode()) for (JsonNode child : node) { Integer result = firstIntegerFromTree(child); if (result != null) return result; }
        return null;
    }

    private static Integer secondsFromTree(JsonNode node) {
        if (node == null) return null;
        if (node.isValueNode()) {
            String value = node.asText("");
            if (value.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) return secondsValue(value);
        }
        if (node.isContainerNode()) for (JsonNode child : node) { Integer result = secondsFromTree(child); if (result != null) return result; }
        return null;
    }

    private static Integer secondsValue(String value) {
        String[] parts = value.split(":"); int result = 0;
        for (String part : parts) result = result * 60 + Integer.parseInt(part);
        return result;
    }

    private void collectResults(JsonNode node, List<JsonNode> out) {
        if (node == null) return;
        if (node.isObject()) { if (node.has("result_id") || node.has("resultId") || node.has("registration_id")) out.add(node); node.fields().forEachRemaining(e -> collectResults(e.getValue(), out)); }
        else if (node.isArray()) node.forEach(n -> collectResults(n, out));
    }

    private static String first(JsonNode row, String... keys) { for (String key : keys) if (row.hasNonNull(key)) return row.get(key).asText(); return null; }
    private static Integer integer(JsonNode row, String... keys) { String value = first(row, keys); try { return value == null ? null : Integer.valueOf(value.replaceAll("[^0-9]", "")); } catch (Exception ignored) { return null; } }
    private static Integer seconds(JsonNode row, String... keys) {
        String value = first(row, keys); if (value == null) return null;
        try { if (value.matches("\\d+")) return Integer.valueOf(value); String[] p = value.split(":"); int s=0; for (String part:p) s=s*60+Integer.parseInt(part.replaceAll("[^0-9]", "")); return s; } catch (Exception ignored) { return null; }
    }
    private static String enc(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
}
