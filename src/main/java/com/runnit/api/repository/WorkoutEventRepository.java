package com.runnit.api.repository;

import com.runnit.api.model.WorkoutEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface WorkoutEventRepository extends JpaRepository<WorkoutEvent, Long> {
    List<WorkoutEvent> findByUserIdAndPlannedDateBetweenOrderByPlannedDateAsc(
            Long userId, LocalDate startDate, LocalDate endDate);
}
