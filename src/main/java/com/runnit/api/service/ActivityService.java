package com.runnit.api.service;

import com.runnit.api.dto.ActivityRequest;
import com.runnit.api.model.Activity;
import com.runnit.api.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {
    
    private final ActivityRepository activityRepository;
    
    @Transactional
    public Activity createActivity(Long userId, ActivityRequest request) {
        Activity activity = Activity.builder()
                .userId(userId)
                .sportType(request.getSportType())
                .durationSeconds(request.getDurationSeconds())
                .distanceMeters(request.getDistanceMeters())
                .source(Activity.Source.MANUAL)
                .build();
        
        return activityRepository.save(activity);
    }
    
    public Page<Activity> getUserActivities(Long userId, int page, int size) {
        return activityRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
    }
    
    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }
}