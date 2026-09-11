package com.runnit.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.runnit.api.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${athlinks.api.key:}") private String athlinksKey;
    @Value("${runsignup.api.key:}") private String runSignupKey;
    @Value("${runsignup.api.secret:}") private String runSignupSecret;
    @Value("${runsignup.api.caller-token:}") private String runSignupCallerToken;
    @Value("${runsignup.api.caller-secret:}") private String runSignupCallerSecret;

    public RaceResultDiscoveryService(RestTemplate restTemplate, ObjectMapper mapper) { this.restTemplate = restTemplate; this.mapper = mapper; }

    public List<Map<String,Object>> discover(User user, String provider, String raceId, String eventId) {
        if ("ATHLINKS".equalsIgnoreCase(provider)) return athlinks(user);
        if ("RUNSIGNUP".equalsIgnoreCase(provider)) return runSignup(user, raceId, eventId);
        return List.of();
    }

    public List<String> providers() { return List.of("ATHLINKS", "RUNSIGNUP"); }

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
        String url = "https://api.runsignup.com/rest/race/" + enc(raceId) + "/results/get-results?format=json&api_key=" + enc(runSignupKey) + "&api_secret=" + enc(runSignupSecret) + "&event_id=" + enc(eventId) + "&first_name=" + enc(first) + "&last_name=" + enc(last) + "&results_per_page=100&rsu_api_reg=" + enc(runSignupCallerToken);
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-RSU-API-REG-SECRET", runSignupCallerSecret);
            String body = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, new org.springframework.http.HttpEntity<>(headers), String.class).getBody();
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
