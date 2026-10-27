package com.runnit.api.repository;

import com.runnit.api.model.NutritionEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NutritionEntryRepository extends JpaRepository<NutritionEntry, Long> {
    List<NutritionEntry> findByUserIdAndLoggedAtGreaterThanEqualAndLoggedAtLessThanOrderByLoggedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    Optional<NutritionEntry> findByIdAndUserId(Long id, Long userId);
}
