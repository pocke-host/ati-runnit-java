package com.runnit.api.service;

import com.runnit.api.dto.ActivityRequest;
import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {
    
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    
    @Transactional
    public Activity createActivity(Long userId, ActivityRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Activity activity = Activity.builder()
                .user(user)
                .sportType(request.getSportType())
                .durationSeconds(request.getDurationSeconds())
                .distanceMeters(request.getDistanceMeters())
                .elevationGain(request.getElevationGain())
                .calories(request.getCalories())
                .averageHeartRate(request.getAverageHeartRate())
                .averagePace(request.getAveragePace())
                .routePolyline(request.getRoutePolyline())
                .startLat(request.getStartLat())
                .startLng(request.getStartLng())
                .source(Activity.Source.MANUAL)
                .build();

        return activityRepository.save(activity);
    }

    @Transactional(readOnly = true)
    public Page<Activity> getUserActivities(Long userId, int page, int size) {
        return activityRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }

    @Transactional(readOnly = true)
    public Page<Activity> getFeed(Long userId, int page, int size) {
        List<Long> followingIds = followRepository.findFollowingUserIds(userId);
        followingIds.add(userId);
        return activityRepository.findFeedByUserIds(followingIds, PageRequest.of(page, size));
    }
}