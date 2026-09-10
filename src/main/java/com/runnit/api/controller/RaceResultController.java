package com.runnit.api.controller;

import com.runnit.api.model.RaceResult;
import com.runnit.api.repository.RaceResultRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.service.RaceResultDiscoveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/race-results")
@RequiredArgsConstructor
public class RaceResultController {
    private final RaceResultRepository repository;
    private final UserRepository userRepository;
    private final RaceResultDiscoveryService discoveryService;

    @GetMapping("/discover")
    public ResponseEntity<?> discover(@RequestParam(defaultValue = "ATHLINKS") String provider, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            return ResponseEntity.ok(discoveryService.discover(userRepository.findById(userId).orElseThrow(), provider));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<Map<String,Object>> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return repository.findByUserIdOrderByRaceDateDesc(userId).stream().map(this::toMap).collect(Collectors.toList());
    }

    /** Import a result from an official provider, preserving the provider and verification URL. */
    @PostMapping("/import")
    @Transactional
    public ResponseEntity<?> importResult(@RequestBody Map<String,Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String source = text(body, "source", "OFFICIAL");
            String externalId = text(body, "externalResultId", null);
            if (externalId != null && repository.findByUserIdAndSourceAndExternalResultId(userId, source, externalId).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Result already imported"));
            }
            String name = text(body, "raceName", null);
            if (name == null || name.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "raceName is required"));
            RaceResult result = new RaceResult();
            result.setUserId(userId); result.setRaceName(name); result.setSource(source); result.setExternalResultId(externalId);
            result.setDistance(text(body, "distance", null)); result.setResultUrl(text(body, "resultUrl", null));
            result.setRaceDate(date(body.get("raceDate"))); result.setFinishTimeSeconds(number(body.get("finishTimeSeconds")));
            result.setPlacement(number(body.get("placement"))); result.setVerified(Boolean.TRUE.equals(body.get("verified")) || externalId != null);
            return ResponseEntity.ok(toMap(repository.save(result)));
        } catch (Exception e) { return ResponseEntity.badRequest().body(Map.of("error", e.getMessage())); }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        RaceResult result = repository.findById(id).orElseThrow(() -> new RuntimeException("Result not found"));
        if (!userId.equals(result.getUserId())) return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
        repository.delete(result); return ResponseEntity.ok(Map.of("message", "Deleted"));
    }
    private static String text(Map<String,Object> b,String k,String d){Object v=b.get(k); return v == null ? d : String.valueOf(v);}
    private static Integer number(Object v){return v instanceof Number ? ((Number)v).intValue() : (v == null ? null : Integer.valueOf(String.valueOf(v)));}
    private static LocalDate date(Object v){return v == null || String.valueOf(v).isBlank() ? null : LocalDate.parse(String.valueOf(v));}
    private Map<String,Object> toMap(RaceResult r){Map<String,Object> m=new LinkedHashMap<>(); m.put("id",r.getId());m.put("raceName",r.getRaceName());m.put("raceDate",r.getRaceDate());m.put("distance",r.getDistance());m.put("finishTimeSeconds",r.getFinishTimeSeconds());m.put("placement",r.getPlacement());m.put("source",r.getSource());m.put("externalResultId",r.getExternalResultId());m.put("resultUrl",r.getResultUrl());m.put("verified",r.isVerified());m.put("createdAt",r.getCreatedAt());return m;}
}
