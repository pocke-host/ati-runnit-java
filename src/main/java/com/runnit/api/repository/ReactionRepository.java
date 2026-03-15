package com.runnit.api.repository;

import com.runnit.api.model.Reaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByMomentIdAndUserId(Long momentId, Long userId);

    @Query("SELECT r FROM Reaction r JOIN FETCH r.user WHERE r.moment.id = :momentId")
    List<Reaction> findByMomentId(@Param("momentId") Long momentId);

    @Query("SELECT r FROM Reaction r JOIN FETCH r.user WHERE r.moment.id IN :momentIds")
    List<Reaction> findByMomentIdIn(@Param("momentIds") List<Long> momentIds);

    void deleteByMomentIdAndUserId(Long momentId, Long userId);
    long countByMomentId(Long momentId);
}