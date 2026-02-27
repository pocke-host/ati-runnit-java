package com.runnit.api.repository;

import com.runnit.api.model.Challenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    Page<Challenge> findByIsPublicTrue(Pageable pageable);
    Page<Challenge> findByCreatorId(Long creatorId, Pageable pageable);

    @Query("SELECT c FROM Challenge c WHERE c.isPublic = true AND c.endDate >= :today ORDER BY c.startDate ASC")
    Page<Challenge> findActiveChallenges(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT c FROM Challenge c JOIN ChallengeParticipant cp ON cp.challenge = c WHERE cp.user.id = :userId ORDER BY c.startDate DESC")
    List<Challenge> findChallengesForUser(@Param("userId") Long userId);
}
