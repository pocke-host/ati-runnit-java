package com.runnit.api.service;

import com.runnit.api.dto.ActivityRequest;
import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityService {
    
    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public Activity createActivity(Long userId, ActivityRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Activity activity = Activity.builder()
                .user(user)  // Use User object, not userId
                .sportType(request.getSportType())
                .durationSeconds(request.getDurationSeconds())
                .distanceMeters(request.getDistanceMeters())
                .source(Activity.Source.MANUAL)
                .build();
        
        return activityRepository.save(activity);
    }
    
    public Page<Activity> getUserActivities(Long userId, int page, int size) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
            return activityRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), PageRequest.of(page, size));
    }
    
    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Activity not found"));
    }
}