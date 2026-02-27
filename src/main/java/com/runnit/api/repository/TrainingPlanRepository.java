package com.runnit.api.repository;

import com.runnit.api.model.TrainingPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrainingPlanRepository extends JpaRepository<TrainingPlan, Long> {
    Page<TrainingPlan> findByIsPublishedTrue(Pageable pageable);
    Page<TrainingPlan> findByIsPublishedTrueAndSportType(String sportType, Pageable pageable);
    Page<TrainingPlan> findByCreatorId(Long creatorId, Pageable pageable);
    Page<TrainingPlan> findByIsPublishedTrueAndIsVerifiedTrue(Pageable pageable);
}
