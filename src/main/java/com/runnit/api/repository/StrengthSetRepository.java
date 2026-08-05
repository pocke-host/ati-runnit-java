package com.runnit.api.repository;

import com.runnit.api.model.StrengthSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StrengthSetRepository extends JpaRepository<StrengthSet, Long> {
    List<StrengthSet> findByExerciseIdInOrderBySetNumberAsc(List<Long> exerciseIds);

    // Row shape: [0] activityId (Long), [1] performedAt (LocalDateTime), [2] reps (Integer),
    // [3] weightKg (Double), [4] isWarmup (Boolean). Projected via JPQL instead of navigating the
    // LAZY exercise/activity associations, which would throw outside an open Hibernate session.
    @Query("SELECT a.id, COALESCE(a.performedAt, a.createdAt), s.reps, s.weightKg, s.isWarmup " +
           "FROM StrengthSet s JOIN s.exercise se JOIN se.activity a " +
           "WHERE a.user.id = :userId AND LOWER(se.exerciseName) = LOWER(:exerciseName)")
    List<Object[]> findSetRowsByUserAndExerciseName(@Param("userId") Long userId, @Param("exerciseName") String exerciseName);

    // Same row shape as above, scoped to a trailing time window instead of one exercise.
    @Query("SELECT a.id, COALESCE(a.performedAt, a.createdAt), s.reps, s.weightKg, s.isWarmup " +
           "FROM StrengthSet s JOIN s.exercise se JOIN se.activity a " +
           "WHERE a.user.id = :userId AND COALESCE(a.performedAt, a.createdAt) >= :since")
    List<Object[]> findSetRowsByUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
