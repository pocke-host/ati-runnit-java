package com.runnit.api.repository;

import com.runnit.api.model.TrainingPlanWorkout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingPlanWorkoutRepository extends JpaRepository<TrainingPlanWorkout, Long> {
    List<TrainingPlanWorkout> findByPlanIdOrderByWeekNumberAscDayOfWeekAsc(Long planId);
    List<TrainingPlanWorkout> findByPlanIdAndWeekNumberOrderByDayOfWeekAsc(Long planId, int weekNumber);
}
