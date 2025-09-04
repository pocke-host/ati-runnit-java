package com.runnit.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthController {
  @GetMapping("/api/health")
  public Map<String, Object> health() {
    return Map.of("ok", true, "time", System.currentTimeMillis());
  }
}
