package com.runnit.api.repository;

import com.runnit.api.model.WellnessDaily;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WellnessDailyRepository extends JpaRepository<WellnessDaily, Long> {
    Optional<WellnessDaily> findByUserIdAndDate(Long userId, LocalDate date);
    List<WellnessDaily> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end);
}
