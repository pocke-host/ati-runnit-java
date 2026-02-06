package com.runnit.api.repository;

import com.runnit.api.model.Moment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MomentRepository extends JpaRepository<Moment, Long> {
    Optional<Moment> findByUserIdAndDayKey(Long userId, LocalDate dayKey);
    
    @Query("""
        SELECT m FROM Moment m 
        WHERE m.userId IN :userIds 
        ORDER BY m.createdAt DESC
    """)
    Page<Moment> findFeedByUserIds(@Param("userIds") List<Long> userIds, Pageable pageable);
    
    Page<Moment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}