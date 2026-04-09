package com.runnit.api.repository;

import com.runnit.api.model.ActivityReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityReactionRepository extends JpaRepository<ActivityReaction, Long> {
    Optional<ActivityReaction> findByActivityIdAndUserId(Long activityId, Long userId);
    void deleteByActivityIdAndUserId(Long activityId, Long userId);

    @Query("SELECT r.activity.id, COUNT(r) FROM ActivityReaction r WHERE r.activity.id IN :ids GROUP BY r.activity.id")
    List<Object[]> countGroupedByActivityIds(@Param("ids") List<Long> ids);

    // Returns [activityId, reactionType, count] rows for per-type breakdown
    @Query("SELECT r.activity.id, r.type, COUNT(r) FROM ActivityReaction r WHERE r.activity.id IN :ids GROUP BY r.activity.id, r.type")
    List<Object[]> countGroupedByActivityIdsAndType(@Param("ids") List<Long> ids);

    @Query("SELECT r.activity.id, r.type FROM ActivityReaction r WHERE r.activity.id IN :ids AND r.user.id = :userId")
    List<Object[]> findUserReactionsByActivityIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
