package com.runnit.api.repository;

import com.runnit.api.model.PlanWorkout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanWorkoutRepository extends JpaRepository<PlanWorkout, Long> {
    List<PlanWorkout> findByPlanIdAndCompletedFalseOrderByWeekNumberAscDayAsc(Long planId);
    boolean existsByLinkedActivityId(Long linkedActivityId);
    List<PlanWorkout> findTop3ByPlanIdAndCompletedTrueAndLinkedActivityIdIsNotNullOrderByWeekNumberDescDayDesc(Long planId);
}
