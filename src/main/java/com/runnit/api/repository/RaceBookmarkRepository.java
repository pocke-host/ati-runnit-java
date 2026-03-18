package com.runnit.api.repository;

import com.runnit.api.model.RaceBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RaceBookmarkRepository extends JpaRepository<RaceBookmark, Long> {

    List<RaceBookmark> findByUserIdOrderByRaceDateAsc(Long userId);

    Optional<RaceBookmark> findByUserIdAndExternalRaceId(Long userId, String externalRaceId);
}
