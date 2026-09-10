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

    public RaceResultDiscoveryService(RestTemplate restTemplate, ObjectMapper mapper) { this.restTemplate = restTemplate; this.mapper = mapper; }

    public List<Map<String,Object>> discover(User user, String provider) {
        if ("ATHLINKS".equalsIgnoreCase(provider)) return athlinks(user);
        return List.of();
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

    private static String first(JsonNode row, String... keys) { for (String key : keys) if (row.hasNonNull(key)) return row.get(key).asText(); return null; }
    private static Integer integer(JsonNode row, String... keys) { String value = first(row, keys); try { return value == null ? null : Integer.valueOf(value.replaceAll("[^0-9]", "")); } catch (Exception ignored) { return null; } }
    private static Integer seconds(JsonNode row, String... keys) {
        String value = first(row, keys); if (value == null) return null;
        try { if (value.matches("\\d+")) return Integer.valueOf(value); String[] p = value.split(":"); int s=0; for (String part:p) s=s*60+Integer.parseInt(part.replaceAll("[^0-9]", "")); return s; } catch (Exception ignored) { return null; }
    }
}
