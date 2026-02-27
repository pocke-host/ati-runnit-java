package com.runnit.api.repository;

import com.runnit.api.model.RaceInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RaceInterestRepository extends JpaRepository<RaceInterest, Long> {
    Optional<RaceInterest> findByRaceIdAndUserId(Long raceId, Long userId);
    boolean existsByRaceIdAndUserId(Long raceId, Long userId);
    long countByRaceId(Long raceId);
    List<RaceInterest> findByUserId(Long userId);
}
