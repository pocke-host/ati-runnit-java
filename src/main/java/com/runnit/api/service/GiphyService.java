package com.runnit.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GiphyService {
    private final RestTemplate http;
    private final ObjectMapper mapper;

    @Value("${giphy.api.key:}") private String apiKey;

    public List<Map<String, Object>> search(String query) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalStateException("GIF search is not configured");
        String url = "https://api.giphy.com/v1/gifs/search?api_key=" + enc(apiKey)
                + "&q=" + enc(query) + "&limit=18&rating=pg-13";
        try {
            JsonNode data = mapper.readTree(http.getForObject(url, String.class)).path("data");
            List<Map<String, Object>> result = new ArrayList<>();
            for (JsonNode gif : data) {
                JsonNode images = gif.path("images");
                String imageUrl = images.path("fixed_width").path("url").asText(null);
                if (imageUrl == null || imageUrl.isBlank()) imageUrl = images.path("original").path("url").asText(null);
                if (imageUrl == null || imageUrl.isBlank()) continue;
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("id", gif.path("id").asText(null));
                row.put("title", gif.path("title").asText("GIF"));
                row.put("url", imageUrl);
                result.add(row);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("GIF search failed", e);
        }
    }

    private String enc(String value) { return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8); }
}
