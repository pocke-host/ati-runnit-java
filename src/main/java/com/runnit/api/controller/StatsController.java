package com.runnit.api.controller;

import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.PersonalRecordRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final PersonalRecordRepository personalRecordRepository;

    @GetMapping
    public ResponseEntity<?> getStats() {
        long athleteCount = userRepository.count();
        Long totalMeters = activityRepository.sumDistanceMeters();
        long totalMiles = totalMeters != null ? totalMeters / 1609 : 0;
        long totalPRs = personalRecordRepository.count();

        return ResponseEntity.ok(Map.of(
                "athleteCount", athleteCount,
                "totalMilesLogged", totalMiles,
                "totalPRs", totalPRs,
                "avgRating", 4.9
        ));
    }
}
