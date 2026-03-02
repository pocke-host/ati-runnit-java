package com.runnit.api.repository;

import com.runnit.api.model.ChallengeParticipant;
import com.runnit.api.model.ChallengeParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChallengeParticipantRepository extends JpaRepository<ChallengeParticipant, ChallengeParticipantId> {
    List<ChallengeParticipant> findByChallengeIdOrderByValueDesc(Long challengeId);
    List<ChallengeParticipant> findByUserId(Long userId);
    boolean existsByChallengeIdAndUserId(Long challengeId, Long userId);
}
