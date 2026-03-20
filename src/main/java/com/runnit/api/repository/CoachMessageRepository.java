package com.runnit.api.repository;

import com.runnit.api.model.CoachMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoachMessageRepository extends JpaRepository<CoachMessage, Long> {

    List<CoachMessage> findByCoachIdAndAthleteIdOrderByCreatedAtAsc(Long coachId, Long athleteId);

    long countByCoachIdAndAthleteIdAndSenderIdNotAndIsReadFalse(Long coachId, Long athleteId, Long senderId);

    List<CoachMessage> findByCoachIdAndAthleteIdAndSenderIdNotAndIsReadFalse(Long coachId, Long athleteId, Long senderId);

    /**
     * Count all unread messages for a user across all threads they participate in,
     * excluding messages they sent themselves.
     */
    @Query("SELECT COUNT(m) FROM CoachMessage m " +
           "WHERE (m.coachId = :userId OR m.athleteId = :userId) " +
           "AND m.senderId <> :userId " +
           "AND m.isRead = false")
    long countUnreadForUser(@Param("userId") Long userId);
}
