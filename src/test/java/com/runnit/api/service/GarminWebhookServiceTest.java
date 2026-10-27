package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.repository.WellnessDailyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GarminWebhookServiceTest {

    @Mock UserRepository userRepository;
    @Mock ActivityRepository activityRepository;
    @Mock WellnessDailyRepository wellnessDailyRepository;
    @Mock AdaptivePlanService adaptivePlanService;
    @Mock AutoMomentService autoMomentService;

    @InjectMocks GarminWebhookService service;

    @Test
    void importsActivityWhenIdentityIsAtEnvelopeLevel() {
        User user = new User();
        user.setId(42L);
        when(userRepository.findByGarminUserId("garmin-user-42")).thenReturn(Optional.of(user));
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(activityRepository.existsByUserIdAndExternalId(42L, "garmin_987654")).thenReturn(false);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", "garmin-user-42");
        payload.put("activities", java.util.List.of(new LinkedHashMap<>(Map.of(
                "activityId", 987654L,
                "activityType", "running",
                "startTimeInSeconds", 1_735_689_600L,
                "durationInSeconds", 3_600,
                "distanceInMeters", 10_000,
                "averageHeartRateInBeatsPerMinute", 151,
                "maxHeartRateInBeatsPerMinute", 176,
                "averageRunCadenceInStepsPerMinute", 174.0
        ))));

        assertEquals(1, service.processPayload(payload));

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        Activity saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals(Activity.Source.GARMIN, saved.getSource());
        assertEquals(Activity.SportType.RUN, saved.getSportType());
        assertEquals(10_000, saved.getDistanceMeters());
        assertEquals(174.0, saved.getAverageCadence());
        assertNotNull(saved.getPerformedAt());
        verify(userRepository).save(user);
    }

    @Test
    void importsSingleActivityObjectAndUsesAccessTokenFallback() {
        User user = new User();
        user.setId(7L);
        when(userRepository.findByGarminAccessToken("access-token")).thenReturn(Optional.of(user));
        when(activityRepository.existsByUserIdAndExternalId(7L, "garmin_single-1")).thenReturn(false);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userAccessToken", "access-token");
        payload.put("activities", Map.of(
                "activityId", "single-1",
                "activityType", "cycling",
                "durationInSeconds", 2_400
        ));

        assertEquals(1, service.processPayload(payload));
        verify(activityRepository).save(any(Activity.class));
    }
}
