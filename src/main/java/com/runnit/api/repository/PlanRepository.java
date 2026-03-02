package com.runnit.api.repository;

import com.runnit.api.model.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanRepository extends JpaRepository<Plan, Long> {
    List<Plan> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Plan> findByUserIdAndActiveTrue(Long userId);
}
