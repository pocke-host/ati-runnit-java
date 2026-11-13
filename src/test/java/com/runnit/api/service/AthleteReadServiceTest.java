package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.WellnessDaily;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.WellnessDailyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AthleteReadServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    @Mock
    private WellnessDailyRepository wellnessDailyRepository;

    @Mock
    private TrainingLoadService trainingLoadService;

    @InjectMocks
    private AthleteReadService service;

    @Test
    void sleepInsightUsesPersonalBaselineAndEvidence() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        when(activityRepository.findByUserIdBetween(anyLong(), any(), any())).thenReturn(List.of());
        when(wellnessDailyRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
                .thenReturn(List.of(
                        wellness(today, 300, 80),
                        wellness(today.minusDays(1), 420, 80),
                        wellness(today.minusDays(2), 430, 80),
                        wellness(today.minusDays(3), 410, 80)
                ));
        when(trainingLoadService.computeMetrics(anyLong(), any(ZoneId.class)))
                .thenReturn(Map.of("riskLabel", "BUILDING_BASELINE", "acwr", 0.0, "tsb", 0.0));

        Map<String, Object> result = service.compute(1L, ZoneId.of("UTC"));

        Map<?, ?> primary = (Map<?, ?>) result.get("primary");
        assertEquals("sleep_below_baseline", primary.get("key"));
        assertTrue(((List<?>) primary.get("evidence")).size() >= 3);
    }

    @Test
    void comparableActivitiesProducePerformanceInsight() {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        when(activityRepository.findByUserIdBetween(anyLong(), any(), any())).thenReturn(List.of(
                activity(today.atTime(7, 0), 3600),
                activity(today.minusDays(3).atTime(7, 0), 3300),
                activity(today.minusDays(7).atTime(7, 0), 3320),
                activity(today.minusDays(11).atTime(7, 0), 3280)
        ));
        when(wellnessDailyRepository.findByUserIdAndDateBetweenOrderByDateDesc(anyLong(), any(), any()))
                .thenReturn(List.of());
        when(trainingLoadService.computeMetrics(anyLong(), any(ZoneId.class)))
                .thenReturn(Map.of("riskLabel", "BUILDING_BASELINE", "acwr", 0.0, "tsb", 0.0));

        Map<String, Object> result = service.compute(1L, ZoneId.of("UTC"));

        Map<?, ?> primary = (Map<?, ?>) result.get("primary");
        assertEquals("performance_slipping", primary.get("key"));
        assertEquals("medium", primary.get("confidence"));
    }

    private WellnessDaily wellness(LocalDate date, int sleepMinutes, int recovery) {
        WellnessDaily row = new WellnessDaily();
        row.setDate(date);
        row.setSource("WHOOP");
        row.setTotalSleepMinutes(sleepMinutes);
        row.setRecoveryScore(recovery);
        return row;
    }

    private Activity activity(java.time.LocalDateTime performedAt, int durationSeconds) {
        return Activity.builder()
                .sportType(Activity.SportType.RUN)
                .distanceMeters(10_000)
                .durationSeconds(durationSeconds)
                .performedAt(performedAt)
                .build();
    }
}
