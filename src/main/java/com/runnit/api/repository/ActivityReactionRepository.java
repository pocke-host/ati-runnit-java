package com.runnit.api.repository;

import com.runnit.api.model.ActivityReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityReactionRepository extends JpaRepository<ActivityReaction, Long> {
    Optional<ActivityReaction> findByActivityIdAndUserId(Long activityId, Long userId);
    void deleteByActivityIdAndUserId(Long activityId, Long userId);
}
