package com.runnit.api.repository;

import com.runnit.api.model.WorkoutLibrary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkoutLibraryRepository extends JpaRepository<WorkoutLibrary, Long> {
    List<WorkoutLibrary> findByUserIdOrderByCreatedAtDesc(Long userId);
}
