package com.runnit.api.repository;

import com.runnit.api.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByUserId(Long userId);
    boolean existsByUserIdAndBadgeId(Long userId, String badgeId);
}
