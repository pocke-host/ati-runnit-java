package com.runnit.api.repository;

import com.runnit.api.model.PlanAdaptation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanAdaptationRepository extends JpaRepository<PlanAdaptation, Long> {
    List<PlanAdaptation> findByPlanIdOrderByCreatedAtDesc(Long planId);
    List<PlanAdaptation> findByPlanWorkoutId(Long planWorkoutId);
    boolean existsByTriggerActivityIdAndPlanWorkoutId(Long triggerActivityId, Long planWorkoutId);
}
