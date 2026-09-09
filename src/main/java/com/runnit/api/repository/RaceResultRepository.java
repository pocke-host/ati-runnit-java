package com.runnit.api.repository;

import com.runnit.api.model.RaceResult;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RaceResultRepository extends JpaRepository<RaceResult, Long> {
    List<RaceResult> findByUserIdOrderByRaceDateDesc(Long userId);
    Optional<RaceResult> findByUserIdAndSourceAndExternalResultId(Long userId, String source, String externalResultId);
}
