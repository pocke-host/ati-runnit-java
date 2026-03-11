package com.runnit.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/events")
public class EventsController {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // ── RunSignup proxy ───────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<?> getEvents(
            @RequestParam(required = false) String event_type,
            @RequestParam(required = false) String zipcode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int results_per_page) {
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl("https://runsignup.com/Rest/races")
                    .queryParam("format", "json")
                    .queryParam("future_events_only", "T")
                    .queryParam("page", page)
                    .queryParam("results_per_page", results_per_page);

            if (event_type != null && !event_type.isBlank()) {
                builder.queryParam("event_type", event_type);
            }
            if (zipcode != null && !zipcode.isBlank()) {
                builder.queryParam("zipcode", zipcode).queryParam("radius", "50");
            }

            String url = builder.toUriString();
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("races", new Object[0]));
        }
    }

    // ── FindARace scrape proxy ────────────────────────────────────────────

    // Simple in-memory cache: hold scraped events for 1 hour
    private static final long CACHE_TTL_MS = 60 * 60 * 1000L;
    private final AtomicReference<List<Map<String, Object>>> cachedFar = new AtomicReference<>(null);
    private volatile long cacheTimestamp = 0;

    /** Pages to scrape on findarace.com, mapped to the sport label */
    private static final Map<String, String> FAR_PAGES = new LinkedHashMap<>();
    static {
        FAR_PAGES.put("https://findarace.com/running-events",  "running");
        FAR_PAGES.put("https://findarace.com/marathons",       "running");
        FAR_PAGES.put("https://findarace.com/half-marathons",  "running");
        FAR_PAGES.put("https://findarace.com/10k-runs",        "running");
        FAR_PAGES.put("https://findarace.com/5k-runs",         "running");
        FAR_PAGES.put("https://findarace.com/triathlons",      "triathlon");
    }

    @GetMapping("/findarace")
    public ResponseEntity<?> getFindARaceEvents() {
        try {
            long now = Instant.now().toEpochMilli();
            if (cachedFar.get() != null && (now - cacheTimestamp) < CACHE_TTL_MS) {
                return ResponseEntity.ok(cachedFar.get());
            }

            List<Map<String, Object>> results = new ArrayList<>();
            Set<String> seen = new HashSet<>();

            for (Map.Entry<String, String> entry : FAR_PAGES.entrySet()) {
                String pageUrl = entry.getKey();
                String sport   = entry.getValue();
                try {
                    List<Map<String, Object>> pageEvents = scrapePage(pageUrl, sport);
                    for (Map<String, Object> ev : pageEvents) {
                        String key = (String) ev.get("name") + "|" + ev.get("date");
                        if (seen.add(key)) {
                            results.add(ev);
                        }
                    }
                } catch (Exception ignored) {
                    // skip failed pages — partial results are fine
                }
            }

            // Sort by date ascending
            results.sort((a, b) -> {
                String da = (String) a.get("date");
                String db = (String) b.get("date");
                if (da == null) da = "";
                if (db == null) db = "";
                return da.compareTo(db);
            });

            cachedFar.set(results);
            cacheTimestamp = now;
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            return ResponseEntity.ok(List.of());
        }
    }

    /** Fetch a findarace.com listing page and extract JSON-LD SportsEvent items */
    private List<Map<String, Object>> scrapePage(String pageUrl, String sport) throws Exception {
        Document doc = Jsoup.connect(pageUrl)
                .userAgent("Mozilla/5.0 (compatible; RunnitBot/1.0)")
                .timeout(12_000)
                .get();

        List<Map<String, Object>> events = new ArrayList<>();
        String sportEmoji = sport.equals("triathlon") ? "🏊" : "🏃";
        String sportLabel = sport.equals("triathlon") ? "Triathlon" : "Running";

        // Extract JSON-LD scripts
        Elements scripts = doc.select("script[type=application/ld+json]");
        for (Element script : scripts) {
            String json = script.html().trim();
            if (json.isEmpty()) continue;
            try {
                JsonNode root = objectMapper.readTree(json);
                // Unwrap @graph arrays
                if (root.has("@graph")) root = root.get("@graph");

                // Handle ItemList
                if (root.isArray()) {
                    for (JsonNode node : root) {
                        if ("ItemList".equals(getText(node, "@type"))) {
                            JsonNode items = node.get("itemListElement");
                            if (items != null && items.isArray()) {
                                for (JsonNode listItem : items) {
                                    JsonNode item = listItem.has("item") ? listItem.get("item") : listItem;
                                    Map<String, Object> ev = parseEvent(item, sport, sportEmoji, sportLabel);
                                    if (ev != null) events.add(ev);
                                }
                            }
                        } else if ("SportsEvent".equals(getText(node, "@type"))) {
                            Map<String, Object> ev = parseEvent(node, sport, sportEmoji, sportLabel);
                            if (ev != null) events.add(ev);
                        }
                    }
                } else if ("ItemList".equals(getText(root, "@type"))) {
                    JsonNode items = root.get("itemListElement");
                    if (items != null && items.isArray()) {
                        for (JsonNode listItem : items) {
                            JsonNode item = listItem.has("item") ? listItem.get("item") : listItem;
                            Map<String, Object> ev = parseEvent(item, sport, sportEmoji, sportLabel);
                            if (ev != null) events.add(ev);
                        }
                    }
                } else if ("SportsEvent".equals(getText(root, "@type"))) {
                    Map<String, Object> ev = parseEvent(root, sport, sportEmoji, sportLabel);
                    if (ev != null) events.add(ev);
                }
            } catch (Exception ignored) { /* malformed JSON-LD — skip */ }
        }

        return events;
    }

    private Map<String, Object> parseEvent(JsonNode node, String sport, String sportEmoji, String sportLabel) {
        if (node == null || node.isNull()) return null;
        String type = getText(node, "@type");
        if (type != null && !type.contains("SportsEvent") && !type.contains("Event")) return null;

        String name = getText(node, "name");
        if (name == null || name.isBlank()) return null;

        // Date: "2026-04-15T09:00:00" → "2026-04-15"
        String rawDate = getText(node, "startDate");
        String date = rawDate != null && rawDate.length() >= 10 ? rawDate.substring(0, 10) : "";

        // Skip past events
        if (!date.isEmpty() && date.compareTo(todayStr()) < 0) return null;

        // Location
        String city  = "";
        String state = "";
        JsonNode loc = node.get("location");
        if (loc != null) {
            city = getText(loc, "name");
            if (city == null) city = "";
            JsonNode addr = loc.get("address");
            if (addr != null) {
                String locality = getText(addr, "addressLocality");
                String region   = getText(addr, "addressRegion");
                if (locality != null && !locality.isBlank()) city  = locality;
                if (region   != null && !region.isBlank())   state = region;
            }
        }

        // URL
        String url = getText(node, "url");
        if (url == null || url.isBlank()) url = "";
        // Ensure absolute URL
        if (url.startsWith("/")) url = "https://findarace.com" + url;

        // Description
        String description = getText(node, "description");
        if (description == null) description = "";
        description = description.replaceAll("<[^>]+>", "").trim();
        String summary = description.length() > 140
                ? description.substring(0, 140) + "…"
                : (description.isEmpty() ? name + " in " + (city.isEmpty() ? "the UK" : city) + "." : description);

        // Image
        String image = "";
        JsonNode imgNode = node.get("image");
        if (imgNode != null) {
            if (imgNode.isTextual()) {
                image = imgNode.asText();
            } else if (imgNode.isObject()) {
                image = getText(imgNode, "url");
                if (image == null) image = "";
            }
        }

        // Distances from name
        List<String> distances = extractDistances(name, sport);

        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("id",          "far_" + Math.abs(name.hashCode()) + "_" + date);
        ev.put("name",        name);
        ev.put("date",        date);
        ev.put("sport",       sport);
        ev.put("sportEmoji",  sportEmoji);
        ev.put("sportLabel",  sportLabel);
        ev.put("city",        city);
        ev.put("state",       state);
        ev.put("distances",   distances);
        ev.put("summary",     summary);
        ev.put("url",         url);
        ev.put("registerUrl", url);
        ev.put("image",       image);
        return ev;
    }

    private List<String> extractDistances(String name, String sport) {
        List<String> found = new ArrayList<>();
        String lower = name.toLowerCase();

        if (sport.equals("triathlon")) {
            if (lower.contains("iron distance") || lower.contains("full iron") || lower.contains("140.6")) found.add("Ironman");
            if (lower.contains("70.3") || lower.contains("half iron") || lower.contains("middle distance")) found.add("70.3");
            if (lower.contains("olympic") || lower.contains("standard distance")) found.add("Olympic");
            if (lower.contains("sprint")) found.add("Sprint");
            if (lower.contains("super sprint")) found.add("Super Sprint");
            if (found.isEmpty()) found.add("Triathlon");
        } else {
            if (lower.contains("marathon") && !lower.contains("half")) found.add("Marathon");
            if (lower.contains("half marathon") || lower.contains("half-marathon")) found.add("Half Marathon");
            if (lower.contains("10k") || lower.contains("10 km")) found.add("10K");
            if (lower.contains("5k")  || lower.contains("5 km"))  found.add("5K");
            if (lower.contains("ultra")) found.add("Ultra");
            if (lower.contains("trail")) found.add("Trail");
            if (found.isEmpty()) found.add("Running");
        }

        // Deduplicate, cap at 4
        return new ArrayList<>(new LinkedHashSet<>(found)).subList(0, Math.min(4, found.size()));
    }

    private String getText(JsonNode node, String field) {
        if (node == null) return null;
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) return null;
        return child.isTextual() ? child.asText() : child.toString().replace("\"", "");
    }

    private String todayStr() {
        return java.time.LocalDate.now().toString(); // "YYYY-MM-DD"
    }
}
