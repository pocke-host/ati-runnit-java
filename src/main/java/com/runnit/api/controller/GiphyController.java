package com.runnit.api.controller;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.service.GiphyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/giphy")
@RequiredArgsConstructor
public class GiphyController {
    private final GiphyService giphyService;

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        if (q == null || q.isBlank()) throw new BadRequestException("Query is required");
        return giphyService.search(q.trim());
    }
}
