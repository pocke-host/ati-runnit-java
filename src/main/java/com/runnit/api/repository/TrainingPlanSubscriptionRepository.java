package com.runnit.api.repository;

import com.runnit.api.model.TrainingPlanSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrainingPlanSubscriptionRepository extends JpaRepository<TrainingPlanSubscription, Long> {
    Optional<TrainingPlanSubscription> findByUserIdAndPlanId(Long userId, Long planId);
    List<TrainingPlanSubscription> findByUserIdAndStatus(Long userId, String status);
    List<TrainingPlanSubscription> findByUserId(Long userId);
    boolean existsByUserIdAndPlanId(Long userId, Long planId);
}
