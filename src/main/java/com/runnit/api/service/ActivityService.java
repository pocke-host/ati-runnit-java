package com.runnit.api.service;

import com.runnit.api.dto.ActivityRequest;
import com.runnit.api.dto.FeedActivityDTO;
import com.runnit.api.model.Activity;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityReactionRepository;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.CommentRepository;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ActivityReactionRepository activityReactionRepository;
    private final CommentRepository commentRepository;

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
    public Page<FeedActivityDTO> getFeed(Long userId, int page, int size) {
        List<Long> followingIds = followRepository.findFollowingUserIds(userId);
        followingIds.add(userId);

        Page<Activity> activityPage = activityRepository.findFeedByUserIds(followingIds, PageRequest.of(page, size));
        List<Activity> activities = activityPage.getContent();

        if (activities.isEmpty()) {
            return new PageImpl<>(List.of(), activityPage.getPageable(), activityPage.getTotalElements());
        }

        List<Long> ids = activities.stream().map(Activity::getId).collect(Collectors.toList());

        // Batch load per-type reaction counts — returns [activityId, type, count]
        Map<Long, Map<String, Long>> reactionCountsByType = new HashMap<>();
        activityReactionRepository.countGroupedByActivityIdsAndType(ids).forEach(row -> {
            Long actId = (Long) row[0];
            String type = row[1].toString();
            Long count = (Long) row[2];
            reactionCountsByType.computeIfAbsent(actId, k -> new HashMap<>()).put(type, count);
        });

        // Batch load comment counts — 1 query instead of N
        Map<Long, Long> commentCounts = commentRepository.countGroupedByActivityIds(ids)
                .stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        // Batch load current user's reactions — 1 query instead of N
        Map<Long, String> userReactions = activityReactionRepository.findUserReactionsByActivityIds(ids, userId)
                .stream().collect(Collectors.toMap(r -> (Long) r[0], r -> r[1].toString()));

        List<FeedActivityDTO> dtos = activities.stream().map(a -> {
            FeedActivityDTO dto = FeedActivityDTO.from(a);
            dto.setReactionCounts(reactionCountsByType.getOrDefault(a.getId(), Map.of()));
            dto.setCommentCount(commentCounts.getOrDefault(a.getId(), 0L));
            dto.setUserReaction(userReactions.get(a.getId()));
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, activityPage.getPageable(), activityPage.getTotalElements());
    }
}
