package com.runnit.api.repository;

import com.runnit.api.model.WeeklyPlanAdaptation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyPlanAdaptationRepository extends JpaRepository<WeeklyPlanAdaptation, Long> {
    List<WeeklyPlanAdaptation> findBySubscriptionIdOrderByWeekNumberAsc(Long subscriptionId);
    Optional<WeeklyPlanAdaptation> findBySubscriptionIdAndWeekNumber(Long subscriptionId, int weekNumber);
}
