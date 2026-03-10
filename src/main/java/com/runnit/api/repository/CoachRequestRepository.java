package com.runnit.api.repository;

import com.runnit.api.model.CoachRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoachRequestRepository extends JpaRepository<CoachRequest, Long> {

    List<CoachRequest> findByCoachIdAndStatus(Long coachId, String status);

    List<CoachRequest> findByCoachIdAndStatusNot(Long coachId, String status);

    Optional<CoachRequest> findByCoachIdAndAthleteId(Long coachId, Long athleteId);

    Optional<CoachRequest> findByAthleteIdAndStatus(Long athleteId, String status);

    void deleteByCoachIdAndAthleteId(Long coachId, Long athleteId);

    boolean existsByCoachIdAndAthleteIdAndStatus(Long coachId, Long athleteId, String status);
}
