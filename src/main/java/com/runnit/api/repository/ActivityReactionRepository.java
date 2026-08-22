package com.runnit.api.repository;

import com.runnit.api.model.ActivityReaction;
import com.runnit.api.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityReactionRepository extends JpaRepository<ActivityReaction, Long> {
    // LIKE and KUDOS are independent per user/activity — always scope lookups and deletes
    // by type as well, or one reaction silently clobbers the other.
    Optional<ActivityReaction> findByActivityIdAndUserIdAndType(Long activityId, Long userId, Reaction.ReactionType type);
    void deleteByActivityIdAndUserIdAndType(Long activityId, Long userId, Reaction.ReactionType type);

    @Query("SELECT r.activity.id, COUNT(r) FROM ActivityReaction r WHERE r.activity.id IN :ids GROUP BY r.activity.id")
    List<Object[]> countGroupedByActivityIds(@Param("ids") List<Long> ids);

    // Returns [activityId, reactionType, count] rows for per-type breakdown
    @Query("SELECT r.activity.id, r.type, COUNT(r) FROM ActivityReaction r WHERE r.activity.id IN :ids GROUP BY r.activity.id, r.type")
    List<Object[]> countGroupedByActivityIdsAndType(@Param("ids") List<Long> ids);

    @Query("SELECT r.activity.id, r.type FROM ActivityReaction r WHERE r.activity.id IN :ids AND r.user.id = :userId")
    List<Object[]> findUserReactionsByActivityIds(@Param("ids") List<Long> ids, @Param("userId") Long userId);
}
