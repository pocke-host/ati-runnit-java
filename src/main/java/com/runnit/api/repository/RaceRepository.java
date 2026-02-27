package com.runnit.api.repository;

import com.runnit.api.model.Race;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface RaceRepository extends JpaRepository<Race, Long> {
    Page<Race> findByRaceDateGreaterThanEqualOrderByRaceDateAsc(LocalDate fromDate, Pageable pageable);
    Page<Race> findByIsFeaturedTrueAndRaceDateGreaterThanEqualOrderByRaceDateAsc(LocalDate fromDate, Pageable pageable);

    @Query("SELECT r FROM Race r WHERE r.raceDate >= :fromDate AND (:city IS NULL OR LOWER(r.city) LIKE LOWER(CONCAT('%', :city, '%'))) ORDER BY r.raceDate ASC")
    Page<Race> findByDateAndCity(@Param("fromDate") LocalDate fromDate, @Param("city") String city, Pageable pageable);

    Page<Race> findByRaceType(String raceType, Pageable pageable);
    Page<Race> findByOrganizerUserId(Long organizerUserId, Pageable pageable);
}
