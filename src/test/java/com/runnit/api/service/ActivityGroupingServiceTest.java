package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityGroupingServiceTest {

    @Mock ActivityRepository activityRepository;

    @InjectMocks ActivityGroupingService service;

    @Test
    void linksWhoopToGarminAndKeepsGarminAsCanonicalRouteSource() {
        User user = new User();
        user.setId(42L);

        Activity garmin = activity(user, 100L, Activity.Source.GARMIN,
                LocalDateTime.of(2026, 10, 29, 7, 0), 3_600, 10_000);
        Activity whoop = activity(user, 101L, Activity.Source.WHOOP,
                LocalDateTime.of(2026, 10, 29, 7, 5), 3_540, 9_700);

        when(activityRepository.findByUserIdNearTime(any(), any(), any()))
                .thenReturn(List.of(garmin, whoop));
        when(activityRepository.save(any(Activity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Activity canonical = service.groupIfMatching(whoop);

        assertSame(garmin, canonical);
        assertEquals(100L, whoop.getCanonicalActivityId());
        verify(activityRepository).save(whoop);
    }

    @Test
    void doesNotLinkSeparateWorkoutsOnTheSameDay() {
        User user = new User();
        user.setId(42L);

        Activity garmin = activity(user, 200L, Activity.Source.GARMIN,
                LocalDateTime.of(2026, 10, 29, 7, 0), 3_600, 10_000);
        Activity whoop = activity(user, 201L, Activity.Source.WHOOP,
                LocalDateTime.of(2026, 10, 29, 12, 0), 1_800, 4_000);

        when(activityRepository.findByUserIdNearTime(any(), any(), any()))
                .thenReturn(List.of(garmin, whoop));

        Activity canonical = service.groupIfMatching(whoop);

        assertSame(whoop, canonical);
        assertEquals(null, whoop.getCanonicalActivityId());
    }

    private Activity activity(User user, long id, Activity.Source source,
                              LocalDateTime performedAt, int duration, int distance) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setUser(user);
        activity.setSource(source);
        activity.setSportType(Activity.SportType.RUN);
        activity.setPerformedAt(performedAt);
        activity.setDurationSeconds(duration);
        activity.setDistanceMeters(distance);
        return activity;
    }
}
