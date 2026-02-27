package com.runnit.api.repository;

import com.runnit.api.model.LiveLocationShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LiveLocationShareRepository extends JpaRepository<LiveLocationShare, Long> {
    Optional<LiveLocationShare> findByShareToken(String shareToken);
    List<LiveLocationShare> findByUserIdAndIsActiveTrue(Long userId);
    Optional<LiveLocationShare> findByUserIdAndIsActiveTrueAndActivityId(Long userId, Long activityId);
}
