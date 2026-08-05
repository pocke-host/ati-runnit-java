package com.runnit.api.repository;

import com.runnit.api.model.StrengthExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StrengthExerciseRepository extends JpaRepository<StrengthExercise, Long> {
    List<StrengthExercise> findByActivityIdOrderBySequenceOrderAsc(Long activityId);

    @Query("SELECT DISTINCT se.exerciseName FROM StrengthExercise se WHERE se.activity.user.id = :userId ORDER BY se.exerciseName ASC")
    List<String> findDistinctExerciseNamesByUserId(@Param("userId") Long userId);
}
