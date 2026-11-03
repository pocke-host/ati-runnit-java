package com.runnit.api.service;

import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.repository.WellnessDailyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
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
    @Spy GarminRouteParser routeParser = new GarminRouteParser();
    @Mock RestTemplate restTemplate;

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

    @Test
    void convertsActivityDetailsSamplesIntoTheRouteFormatUsedByTheMap() {
        User user = new User();
        user.setId(11L);
        when(userRepository.findByGarminUserId("garmin-user-11")).thenReturn(Optional.of(user));
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));
        when(activityRepository.existsByUserIdAndExternalId(11L, "garmin_detail-1")).thenReturn(false);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("activityId", "detail-1");
        summary.put("activityType", "running");
        summary.put("durationInSeconds", 1_800);
        summary.put("distanceInMeters", 5_000);
        summary.put("startTimeInSeconds", 1_735_689_600L);
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("userId", "garmin-user-11");
        detail.put("summary", summary);
        detail.put("samples", List.of(
                Map.of("latitudeInDegree", 37.7749, "longitudeInDegree", -122.4194, "elevationInMeters", 10.0),
                Map.of("latitudeInDegree", 37.7750, "longitudeInDegree", -122.4193, "elevationInMeters", 11.0)
        ));

        assertEquals(1, service.processPayload(Map.of("activityDetails", List.of(detail))));

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        Activity saved = captor.getValue();
        assertNotNull(saved.getRoutePolyline());
        assertEquals(37.7749, saved.getStartLat());
        assertEquals(-122.4194, saved.getStartLng());
        assertEquals(Activity.SportType.RUN, saved.getSportType());
    }

    @Test
    void downloadsAndParsesGarminGpxActivityFile() {
        User user = new User();
        user.setId(12L);
        when(userRepository.findByGarminUserId("garmin-user-12")).thenReturn(Optional.of(user));
        when(userRepository.findById(12L)).thenReturn(Optional.of(user));
        when(activityRepository.existsByUserIdAndExternalId(12L, "garmin_file-1")).thenReturn(false);

        String gpx = "<?xml version=\"1.0\"?><gpx><trk><trkseg>"
                + "<trkpt lat=\"37.7749\" lon=\"-122.4194\"><ele>10</ele></trkpt>"
                + "<trkpt lat=\"37.7750\" lon=\"-122.4193\"><ele>11</ele></trkpt>"
                + "</trkseg></trk></gpx>";
        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(byte[].class)))
                .thenReturn(new ResponseEntity<>(gpx.getBytes(java.nio.charset.StandardCharsets.UTF_8), HttpStatus.OK));

        Map<String, Object> file = new LinkedHashMap<>();
        file.put("userId", "garmin-user-12");
        file.put("activityId", "file-1");
        file.put("activityName", "Morning Run");
        file.put("fileType", "GPX");
        file.put("callbackURL", "https://apis.garmin.com/activity-file/file-1");

        assertEquals(1, service.processPayload(Map.of("activityFiles", List.of(file))));
        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());
        assertNotNull(captor.getValue().getRoutePolyline());
        assertEquals(37.7749, captor.getValue().getStartLat());
        assertEquals(Activity.SportType.RUN, captor.getValue().getSportType());
    }
}
