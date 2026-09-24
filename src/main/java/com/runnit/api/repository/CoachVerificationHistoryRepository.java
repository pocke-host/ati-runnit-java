package com.runnit.api.repository;

import com.runnit.api.model.CoachVerificationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CoachVerificationHistoryRepository extends JpaRepository<CoachVerificationHistory, Long> {
    List<CoachVerificationHistory> findByCoachIdOrderByCreatedAtDesc(Long coachId);
}
