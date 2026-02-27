package com.runnit.api.repository;

import com.runnit.api.model.ChallengeParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, Long> {
    Optional<ChallengeParticipant> findByChallengeIdAndUserId(Long challengeId, Long userId);
    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);
    long countByChallengeId(Long challengeId);

    @Query("SELECT cp FROM ChallengeParticipant cp WHERE cp.challenge.id = :challengeId ORDER BY cp.currentValue DESC")
    List<ChallengeParticipant> findLeaderboard(@Param("challengeId") Long challengeId);
}
