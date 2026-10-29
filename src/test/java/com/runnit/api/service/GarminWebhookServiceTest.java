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
    @Mock ActivityGroupingService activityGroupingService;
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
        Map<String, Object> activityRecord = new LinkedHashMap<>();
        activityRecord.put("activityId", 987654L);
        activityRecord.put("activityType", "running");
        activityRecord.put("startTimeInSeconds", 1_735_689_600L);
        activityRecord.put("durationInSeconds", 3_600);
        activityRecord.put("distanceInMeters", 10_000);
        activityRecord.put("averageHeartRateInBeatsPerMinute", 151);
        activityRecord.put("maxHeartRateInBeatsPerMinute", 176);
        activityRecord.put("averageRunCadenceInStepsPerMinute", 174.0);
        activityRecord.put("geoPolyline", "encoded-route");
        activityRecord.put("startLatitudeInDegree", 37.7749);
        activityRecord.put("startLongitudeInDegree", -122.4194);
        payload.put("activities", java.util.List.of(activityRecord));

        assertEquals(1, service.processPayload(payload));

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        Activity saved = captor.getValue();
        assertEquals(user, saved.getUser());
        assertEquals(Activity.Source.GARMIN, saved.getSource());
        assertEquals(Activity.SportType.RUN, saved.getSportType());
        assertEquals(10_000, saved.getDistanceMeters());
        assertEquals(174.0, saved.getAverageCadence());
        assertEquals("encoded-route", saved.getRoutePolyline());
        assertEquals(37.7749, saved.getStartLat());
        assertEquals(-122.4194, saved.getStartLng());
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

    @Test
    void enrichesAnActivityWhenGarminDeliversACompleteRecordAfterAPartialRecord() {
        User user = new User();
        user.setId(9L);
        when(userRepository.findByGarminUserId("garmin-user-9")).thenReturn(Optional.of(user));
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));
        when(activityRepository.existsByUserIdAndExternalId(9L, "garmin_123")).thenReturn(true);

        Activity partial = new Activity();
        partial.setUser(user);
        partial.setExternalId("garmin_123");
        partial.setSource(Activity.Source.GARMIN);
        partial.setSportType(Activity.SportType.RUN);
        partial.setDurationSeconds(0);
        when(activityRepository.findByUserIdAndExternalId(9L, "garmin_123")).thenReturn(Optional.of(partial));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("userId", "garmin-user-9");
        payload.put("activities", java.util.List.of(new LinkedHashMap<>(Map.of(
                "activityId", 123L,
                "activityType", "running",
                "startTimeInSeconds", 1_735_689_600L,
                "durationInSeconds", 1_800,
                "distanceInMeters", 5_000,
                "maxHeartRateInBeatsPerMinute", 178
        ))));

        assertEquals(0, service.processPayload(payload));
        assertEquals(1_800, partial.getDurationSeconds());
        assertEquals(5_000, partial.getDistanceMeters());
        assertEquals(178, partial.getMaxHeartRate());
        verify(activityRepository).save(partial);
        verifyNoInteractions(adaptivePlanService, autoMomentService);
    }
}
