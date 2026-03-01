package com.runnit.api.controller;

import com.runnit.api.model.Activity;
import com.runnit.api.model.PersonalRecord;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.PersonalRecordRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/personal-records")
@RequiredArgsConstructor
public class PersonalRecordController {

    private final PersonalRecordRepository prRepository;
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getPersonalRecords(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            PersonalRecord pr = prRepository.findByUserId(userId).orElse(null);
            return ResponseEntity.ok(toMap(pr));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/check")
    @Transactional
    public ResponseEntity<?> checkAndUpdate(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Activity> activities = activityRepository
                    .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, Integer.MAX_VALUE))
                    .getContent();

            PersonalRecord pr = prRepository.findByUserId(userId)
                    .orElse(PersonalRecord.builder().user(user).build());

            for (Activity a : activities) {
                if (a.getDistanceMeters() != null) {
                    String sport = a.getSportType().name();
                    int dist = a.getDistanceMeters();

                    if ("RUN".equals(sport)) {
                        if (dist >= 5000 && (pr.getBest5k() == null || a.getDurationSeconds() < pr.getBest5k()))
                            pr.setBest5k(a.getDurationSeconds());
                        if (dist >= 10000 && (pr.getBest10k() == null || a.getDurationSeconds() < pr.getBest10k()))
                            pr.setBest10k(a.getDurationSeconds());
                        if (dist >= 21097 && (pr.getBestHalf() == null || a.getDurationSeconds() < pr.getBestHalf()))
                            pr.setBestHalf(a.getDurationSeconds());
                        if (dist >= 42195 && (pr.getBestMarathon() == null || a.getDurationSeconds() < pr.getBestMarathon()))
                            pr.setBestMarathon(a.getDurationSeconds());
                        if (pr.getLongestRun() == null || dist > pr.getLongestRun())
                            pr.setLongestRun(dist);
                    }
                    if ("BIKE".equals(sport) && (pr.getLongestRide() == null || dist > pr.getLongestRide()))
                        pr.setLongestRide(dist);
                }
                if (a.getElevationGain() != null && (pr.getMostElevation() == null || a.getElevationGain() > pr.getMostElevation()))
                    pr.setMostElevation(a.getElevationGain());
                if (a.getAveragePace() != null && (pr.getFastestPace() == null || a.getAveragePace() < pr.getFastestPace()))
                    pr.setFastestPace(a.getAveragePace());
            }

            pr.setUpdatedAt(Instant.now());
            prRepository.save(pr);
            return ResponseEntity.ok(toMap(pr));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private Map<String, Object> toMap(PersonalRecord pr) {
        Map<String, Object> map = new HashMap<>();
        if (pr == null) return map;
        map.put("best_5k", pr.getBest5k());
        map.put("best_10k", pr.getBest10k());
        map.put("best_half", pr.getBestHalf());
        map.put("best_marathon", pr.getBestMarathon());
        map.put("longest_run", pr.getLongestRun());
        map.put("longest_ride", pr.getLongestRide());
        map.put("most_elevation", pr.getMostElevation());
        map.put("fastest_pace", pr.getFastestPace());
        return map;
    }
}
