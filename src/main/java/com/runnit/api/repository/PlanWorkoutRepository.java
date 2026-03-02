package com.runnit.api.repository;

import com.runnit.api.model.PlanWorkout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanWorkoutRepository extends JpaRepository<PlanWorkout, Long> {
}
