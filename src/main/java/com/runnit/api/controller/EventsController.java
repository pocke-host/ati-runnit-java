package com.runnit.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/events")
public class EventsController {

    private final RestTemplate restTemplate = new RestTemplate();

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
}
