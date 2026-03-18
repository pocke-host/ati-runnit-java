package com.runnit.api.repository;

import com.runnit.api.model.CoachMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachMessageRepository extends JpaRepository<CoachMessage, Long> {

    List<CoachMessage> findByCoachIdAndAthleteIdOrderByCreatedAtAsc(Long coachId, Long athleteId);

    long countByCoachIdAndAthleteIdAndSenderIdNotAndIsReadFalse(Long coachId, Long athleteId, Long senderId);

    List<CoachMessage> findByCoachIdAndAthleteIdAndSenderIdNotAndIsReadFalse(Long coachId, Long athleteId, Long senderId);
}
