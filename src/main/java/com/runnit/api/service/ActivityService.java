package com.runnit.api.service;

import com.runnit.api.dto.ActivityRequest;
import com.runnit.api.dto.FeedActivityDTO;
import com.runnit.api.dto.StrengthActivityRequest;
import com.runnit.api.dto.StrengthExerciseDTO;
import com.runnit.api.dto.StrengthExerciseRequest;
import com.runnit.api.dto.StrengthSetRequest;
import com.runnit.api.model.Activity;
import com.runnit.api.model.StrengthExercise;
import com.runnit.api.model.StrengthSet;
import com.runnit.api.model.User;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.repository.ActivityReactionRepository;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.CommentRepository;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.repository.StrengthExerciseRepository;
import com.runnit.api.repository.StrengthSetRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final ActivityReactionRepository activityReactionRepository;
    private final CommentRepository commentRepository;
    private final AdaptivePlanService adaptivePlanService;
    private final StrengthExerciseRepository strengthExerciseRepository;
    private final StrengthSetRepository strengthSetRepository;

    @Transactional
    public Activity createActivity(Long userId, ActivityRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Activity activity = Activity.builder()
                .user(user)
                .sportType(request.getSportType())
                .durationSeconds(request.getDurationSeconds())
                .distanceMeters(request.getDistanceMeters())
                .elevationGain(request.getElevationGain())
                .calories(request.getCalories())
                .averageHeartRate(request.getAverageHeartRate())
                .maxHeartRate(request.getMaxHeartRate())
                .averagePace(request.getAveragePace())
                .routePolyline(request.getRoutePolyline())
                .startLat(request.getStartLat())
                .startLng(request.getStartLng())
                .notes(request.getNotes())
                .source(Activity.Source.MANUAL)
                .build();

        Activity saved = activityRepository.save(activity);
        try {
            adaptivePlanService.onActivityRecorded(saved);
        } catch (Exception e) {
            log.warn("Adaptive plan evaluation failed for activity {}: {}", saved.getId(), e.getMessage());
        }
        return saved;
    }

    /**
     * A separate endpoint/method rather than overloading createActivity+ActivityRequest —
     * that DTO is tightly bound to endurance semantics (distance/pace), and bolting a
     * nested exercises list onto it would mean sport-conditional validation inside one
     * flat request shape. This keeps the existing run/bike/swim flow completely
     * unaffected.
     */
    @Transactional
    public Activity createStrengthActivity(Long userId, StrengthActivityRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Activity activity = Activity.builder()
                .user(user)
                .sportType(Activity.SportType.STRENGTH)
                .durationSeconds(request.getDurationSeconds())
                .calories(request.getCalories())
                .notes(request.getNotes())
                .source(Activity.Source.MANUAL)
                .build();
        activity = activityRepository.save(activity);

        int exerciseOrder = 0;
        for (StrengthExerciseRequest exerciseReq : request.getExercises()) {
            StrengthExercise exercise = StrengthExercise.builder()
                    .activity(activity)
                    .exerciseName(exerciseReq.getExerciseName().trim())
                    .sequenceOrder(exerciseReq.getSequenceOrder() != null ? exerciseReq.getSequenceOrder() : exerciseOrder++)
                    .notes(exerciseReq.getNotes())
                    .build();
            exercise = strengthExerciseRepository.save(exercise);

            int setNumber = 1;
            for (StrengthSetRequest setReq : exerciseReq.getSets()) {
                strengthSetRepository.save(StrengthSet.builder()
                        .exercise(exercise)
                        .setNumber(setReq.getSetNumber() != null ? setReq.getSetNumber() : setNumber++)
                        .reps(setReq.getReps())
                        .weightKg(setReq.getWeightKg())
                        .isWarmup(Boolean.TRUE.equals(setReq.getIsWarmup()))
                        .rpe(setReq.getRpe())
                        .build());
            }
        }

        try {
            adaptivePlanService.onActivityRecorded(activity);
        } catch (Exception e) {
            log.warn("Adaptive plan evaluation failed for strength activity {}: {}", activity.getId(), e.getMessage());
        }
        return activity;
    }

    /** Batched two-query load (all exercises, then all their sets in one IN query) — same
     * pattern getUserActivities/getFeed already use for reaction/comment counts, avoids N+1. */
    @Transactional(readOnly = true)
    public List<StrengthExerciseDTO> getStrengthExercises(Long activityId) {
        List<StrengthExercise> exercises = strengthExerciseRepository.findByActivityIdOrderBySequenceOrderAsc(activityId);
        if (exercises.isEmpty()) return List.of();

        List<Long> exerciseIds = exercises.stream().map(StrengthExercise::getId).collect(Collectors.toList());
        Map<Long, List<StrengthSet>> setsByExercise = strengthSetRepository
                .findByExerciseIdInOrderBySetNumberAsc(exerciseIds)
                .stream().collect(Collectors.groupingBy(s -> s.getExercise().getId()));

        return exercises.stream()
                .map(e -> StrengthExerciseDTO.from(e, setsByExercise.getOrDefault(e.getId(), List.of())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<FeedActivityDTO> getUserActivities(Long userId, int page, int size, Long viewerUserId) {
        Page<Activity> activityPage = activityRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        List<Activity> activities = activityPage.getContent();

        if (activities.isEmpty()) {
            return new PageImpl<>(List.of(), activityPage.getPageable(), activityPage.getTotalElements());
        }

        List<Long> ids = activities.stream().map(Activity::getId).collect(Collectors.toList());

        // Batch load per-type reaction counts
        Map<Long, Map<String, Long>> reactionCountsByType = new HashMap<>();
        activityReactionRepository.countGroupedByActivityIdsAndType(ids).forEach(row -> {
            Long actId = (Long) row[0];
            String type = row[1].toString();
            Long count = (Long) row[2];
            reactionCountsByType.computeIfAbsent(actId, k -> new HashMap<>()).put(type, count);
        });

        // Batch load comment counts
        Map<Long, Long> commentCounts = commentRepository.countGroupedByActivityIds(ids)
                .stream().collect(Collectors.toMap(r -> (Long) r[0], r -> (Long) r[1]));

        // Batch load viewer's reactions (null-safe — viewer may be null for public profile views).
        // A user can now hold both LIKE and KUDOS on the same activity, so this groups into a
        // set per activity rather than a single value — toMap would throw on the second row.
        Map<Long, Set<String>> userReactions = new HashMap<>();
        if (viewerUserId != null) {
            activityReactionRepository.findUserReactionsByActivityIds(ids, viewerUserId).forEach(row -> {
                Long actId = (Long) row[0];
                userReactions.computeIfAbsent(actId, k -> new HashSet<>()).add(row[1].toString());
            });
        }

        List<FeedActivityDTO> dtos = activities.stream().map(a -> {
            FeedActivityDTO dto = FeedActivityDTO.from(a);
            dto.setReactionCounts(reactionCountsByType.getOrDefault(a.getId(), Map.of()));
            dto.setCommentCount(commentCounts.getOrDefault(a.getId(), 0L));
            dto.setUserReactions(userReactions.getOrDefault(a.getId(), Set.of()));
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, activityPage.getPageable(), activityPage.getTotalElements());
    }

    @Transactional(readOnly = true)
    public Activity getActivityById(Long id) {
        return activityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Activity not found"));
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

        // Batch load current user's reactions — 1 query instead of N. Grouped into a set per
        // activity since a user can hold both LIKE and KUDOS at once (toMap would throw on
        // the second row for the same activity).
        Map<Long, Set<String>> userReactions = new HashMap<>();
        activityReactionRepository.findUserReactionsByActivityIds(ids, userId).forEach(row -> {
            Long actId = (Long) row[0];
            userReactions.computeIfAbsent(actId, k -> new HashSet<>()).add(row[1].toString());
        });

        List<FeedActivityDTO> dtos = activities.stream().map(a -> {
            FeedActivityDTO dto = FeedActivityDTO.from(a);
            dto.setReactionCounts(reactionCountsByType.getOrDefault(a.getId(), Map.of()));
            dto.setCommentCount(commentCounts.getOrDefault(a.getId(), 0L));
            dto.setUserReactions(userReactions.getOrDefault(a.getId(), Set.of()));
            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, activityPage.getPageable(), activityPage.getTotalElements());
    }
}
