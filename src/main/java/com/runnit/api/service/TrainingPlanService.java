package com.runnit.api.service;

import com.runnit.api.dto.*;
import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingPlanService {

    private final TrainingPlanRepository planRepository;
    private final TrainingPlanWorkoutRepository workoutRepository;
    private final TrainingPlanSubscriptionRepository subscriptionRepository;
    private final WeeklyPlanAdaptationRepository adaptationRepository;
    private final UserRepository userRepository;

    // ─── Plan CRUD ──────────────────────────────────────────────────────────

    @Transactional
    public TrainingPlanResponse createPlan(Long creatorId, TrainingPlanRequest request) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TrainingPlan plan = TrainingPlan.builder()
                .creator(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .sportType(request.getSportType())
                .difficultyLevel(request.getDifficultyLevel())
                .durationWeeks(request.getDurationWeeks())
                .isAdaptive(request.isAdaptive())
                .priceCents(request.getPriceCents())
                .tags(request.getTags())
                .coverImageUrl(request.getCoverImageUrl())
                .isPublished(request.isPublished())
                .build();

        plan = planRepository.save(plan);

        // Persist workouts if provided
        if (request.getWorkouts() != null) {
            TrainingPlan finalPlan = plan;
            List<TrainingPlanWorkout> workouts = request.getWorkouts().stream()
                    .map(dto -> workoutFromDTO(dto, finalPlan))
                    .collect(Collectors.toList());
            workoutRepository.saveAll(workouts);
        }

        return toPlanResponse(plan, creatorId, false);
    }

    public Page<TrainingPlanResponse> getPublishedPlans(Long currentUserId, String sportType, int page, int size) {
        Page<TrainingPlan> plans;
        if (sportType != null && !sportType.isBlank()) {
            plans = planRepository.findByIsPublishedTrueAndSportType(sportType, PageRequest.of(page, size));
        } else {
            plans = planRepository.findByIsPublishedTrue(PageRequest.of(page, size));
        }
        return plans.map(p -> toPlanResponse(p, currentUserId, subscriptionRepository.existsByUserIdAndPlanId(currentUserId, p.getId())));
    }

    public TrainingPlanResponse getPlanById(Long planId, Long currentUserId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Training plan not found"));
        boolean isSubscribed = subscriptionRepository.existsByUserIdAndPlanId(currentUserId, planId);
        return toPlanResponse(plan, currentUserId, isSubscribed);
    }

    public Page<TrainingPlanResponse> getMyPlans(Long creatorId, int page, int size) {
        return planRepository.findByCreatorId(creatorId, PageRequest.of(page, size))
                .map(p -> toPlanResponse(p, creatorId, false));
    }

    @Transactional
    public TrainingPlanResponse updatePlan(Long planId, Long userId, TrainingPlanRequest request) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Training plan not found"));
        if (!plan.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to edit this plan");
        }
        plan.setTitle(request.getTitle());
        plan.setDescription(request.getDescription());
        plan.setSportType(request.getSportType());
        plan.setDifficultyLevel(request.getDifficultyLevel());
        plan.setDurationWeeks(request.getDurationWeeks());
        plan.setAdaptive(request.isAdaptive());
        plan.setPriceCents(request.getPriceCents());
        plan.setTags(request.getTags());
        plan.setCoverImageUrl(request.getCoverImageUrl());
        plan.setPublished(request.isPublished());
        plan = planRepository.save(plan);
        return toPlanResponse(plan, userId, false);
    }

    @Transactional
    public void deletePlan(Long planId, Long userId) {
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Training plan not found"));
        if (!plan.getCreator().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this plan");
        }
        planRepository.delete(plan);
    }

    // ─── Subscription ───────────────────────────────────────────────────────

    @Transactional
    public TrainingPlanSubscriptionDTO subscribe(Long userId, Long planId) {
        if (subscriptionRepository.existsByUserIdAndPlanId(userId, planId)) {
            throw new RuntimeException("Already subscribed to this plan");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        TrainingPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Training plan not found"));

        TrainingPlanSubscription sub = TrainingPlanSubscription.builder()
                .user(user)
                .plan(plan)
                .startDate(LocalDate.now())
                .currentWeek(1)
                .status("ACTIVE")
                .build();
        sub = subscriptionRepository.save(sub);

        // Increment subscriber count
        plan.setSubscriberCount(plan.getSubscriberCount() + 1);
        planRepository.save(plan);

        return toSubscriptionDTO(sub);
    }

    @Transactional
    public void unsubscribe(Long userId, Long planId) {
        TrainingPlanSubscription sub = subscriptionRepository.findByUserIdAndPlanId(userId, planId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        subscriptionRepository.delete(sub);

        TrainingPlan plan = planRepository.findById(planId).orElse(null);
        if (plan != null && plan.getSubscriberCount() > 0) {
            plan.setSubscriberCount(plan.getSubscriberCount() - 1);
            planRepository.save(plan);
        }
    }

    public List<TrainingPlanSubscriptionDTO> getMySubscriptions(Long userId) {
        return subscriptionRepository.findByUserIdAndStatus(userId, "ACTIVE")
                .stream().map(this::toSubscriptionDTO).collect(Collectors.toList());
    }

    public TrainingPlanSubscriptionDTO getSubscription(Long subscriptionId, Long userId) {
        TrainingPlanSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        if (!sub.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }
        return toSubscriptionDTO(sub);
    }

    // ─── Adaptive weekly update ──────────────────────────────────────────────

    @Transactional
    public WeeklyPlanAdaptationDTO adaptWeek(Long subscriptionId, Long userId,
                                              int volumeAdjustmentPercent,
                                              String intensityAdjustment,
                                              String notes) {
        TrainingPlanSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        if (!sub.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }

        // Simple rule-based adaptation (in production, this could call an AI API)
        String aiReasoning = buildAdaptationReasoning(volumeAdjustmentPercent, intensityAdjustment);

        WeeklyPlanAdaptation adaptation = WeeklyPlanAdaptation.builder()
                .subscription(sub)
                .weekNumber(sub.getCurrentWeek())
                .adaptationNotes(notes)
                .volumeAdjustmentPercent(volumeAdjustmentPercent)
                .intensityAdjustment(intensityAdjustment)
                .aiReasoning(aiReasoning)
                .build();

        adaptation = adaptationRepository.save(adaptation);
        return toAdaptationDTO(adaptation);
    }

    @Transactional
    public TrainingPlanSubscriptionDTO advanceWeek(Long subscriptionId, Long userId) {
        TrainingPlanSubscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new RuntimeException("Subscription not found"));
        if (!sub.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }
        if (sub.getCurrentWeek() >= sub.getPlan().getDurationWeeks()) {
            sub.setStatus("COMPLETED");
        } else {
            sub.setCurrentWeek(sub.getCurrentWeek() + 1);
        }
        sub = subscriptionRepository.save(sub);
        return toSubscriptionDTO(sub);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private String buildAdaptationReasoning(int volumeAdjust, String intensityAdjust) {
        StringBuilder sb = new StringBuilder("Adaptive plan update: ");
        if (volumeAdjust > 0) sb.append("Increasing volume by ").append(volumeAdjust).append("%. ");
        else if (volumeAdjust < 0) sb.append("Reducing volume by ").append(Math.abs(volumeAdjust)).append("% for recovery. ");
        if (intensityAdjust != null) sb.append("Intensity adjusted to ").append(intensityAdjust).append(".");
        return sb.toString();
    }

    private TrainingPlanResponse toPlanResponse(TrainingPlan plan, Long currentUserId, boolean isSubscribed) {
        List<TrainingPlanWorkoutDTO> workoutDTOs = workoutRepository
                .findByPlanIdOrderByWeekNumberAscDayOfWeekAsc(plan.getId())
                .stream().map(this::toWorkoutDTO).collect(Collectors.toList());

        return TrainingPlanResponse.builder()
                .id(plan.getId())
                .creatorId(plan.getCreator().getId())
                .creatorDisplayName(plan.getCreator().getDisplayName())
                .creatorVerified(plan.getCreator().isVerified())
                .title(plan.getTitle())
                .description(plan.getDescription())
                .sportType(plan.getSportType())
                .difficultyLevel(plan.getDifficultyLevel())
                .durationWeeks(plan.getDurationWeeks())
                .isAdaptive(plan.isAdaptive())
                .isVerified(plan.isVerified())
                .priceCents(plan.getPriceCents())
                .tags(plan.getTags())
                .coverImageUrl(plan.getCoverImageUrl())
                .isPublished(plan.isPublished())
                .subscriberCount(plan.getSubscriberCount())
                .isSubscribed(isSubscribed)
                .workouts(workoutDTOs)
                .createdAt(plan.getCreatedAt())
                .build();
    }

    private TrainingPlanSubscriptionDTO toSubscriptionDTO(TrainingPlanSubscription sub) {
        List<TrainingPlanWorkoutDTO> currentWeekWorkouts = workoutRepository
                .findByPlanIdAndWeekNumberOrderByDayOfWeekAsc(sub.getPlan().getId(), sub.getCurrentWeek())
                .stream().map(this::toWorkoutDTO).collect(Collectors.toList());

        WeeklyPlanAdaptationDTO adaptation = adaptationRepository
                .findBySubscriptionIdAndWeekNumber(sub.getId(), sub.getCurrentWeek())
                .map(this::toAdaptationDTO).orElse(null);

        return TrainingPlanSubscriptionDTO.builder()
                .id(sub.getId())
                .planId(sub.getPlan().getId())
                .planTitle(sub.getPlan().getTitle())
                .planSportType(sub.getPlan().getSportType())
                .planDurationWeeks(sub.getPlan().getDurationWeeks())
                .startDate(sub.getStartDate())
                .currentWeek(sub.getCurrentWeek())
                .status(sub.getStatus())
                .currentWeekWorkouts(currentWeekWorkouts)
                .currentAdaptation(adaptation)
                .createdAt(sub.getCreatedAt())
                .build();
    }

    public TrainingPlanWorkoutDTO toWorkoutDTO(TrainingPlanWorkout w) {
        return TrainingPlanWorkoutDTO.builder()
                .id(w.getId())
                .planId(w.getPlan().getId())
                .weekNumber(w.getWeekNumber())
                .dayOfWeek(w.getDayOfWeek())
                .workoutType(w.getWorkoutType())
                .title(w.getTitle())
                .description(w.getDescription())
                .targetDurationMinutes(w.getTargetDurationMinutes())
                .targetDistanceMeters(w.getTargetDistanceMeters())
                .targetHeartRateZone(w.getTargetHeartRateZone())
                .intensity(w.getIntensity())
                .notes(w.getNotes())
                .createdAt(w.getCreatedAt())
                .build();
    }

    private WeeklyPlanAdaptationDTO toAdaptationDTO(WeeklyPlanAdaptation a) {
        return WeeklyPlanAdaptationDTO.builder()
                .id(a.getId())
                .subscriptionId(a.getSubscription().getId())
                .weekNumber(a.getWeekNumber())
                .adaptationNotes(a.getAdaptationNotes())
                .volumeAdjustmentPercent(a.getVolumeAdjustmentPercent())
                .intensityAdjustment(a.getIntensityAdjustment())
                .aiReasoning(a.getAiReasoning())
                .createdAt(a.getCreatedAt())
                .build();
    }

    private TrainingPlanWorkout workoutFromDTO(TrainingPlanWorkoutDTO dto, TrainingPlan plan) {
        return TrainingPlanWorkout.builder()
                .plan(plan)
                .weekNumber(dto.getWeekNumber())
                .dayOfWeek(dto.getDayOfWeek())
                .workoutType(dto.getWorkoutType())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .targetDurationMinutes(dto.getTargetDurationMinutes())
                .targetDistanceMeters(dto.getTargetDistanceMeters())
                .targetHeartRateZone(dto.getTargetHeartRateZone())
                .intensity(dto.getIntensity() != null ? dto.getIntensity() : "MODERATE")
                .notes(dto.getNotes())
                .build();
    }
}
