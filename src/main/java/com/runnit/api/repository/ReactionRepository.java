package com.runnit.api.repository;

import com.runnit.api.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByMomentIdAndUserId(Long momentId, Long userId);
    List<Reaction> findByMomentId(Long momentId);
    void deleteByMomentIdAndUserId(Long momentId, Long userId);
    long countByMomentId(Long momentId);
}