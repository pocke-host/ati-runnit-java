package com.runnit.api.repository;

import com.runnit.api.model.MultisportEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MultisportEventRepository extends JpaRepository<MultisportEvent, Long> {

    @Query("SELECT e FROM MultisportEvent e WHERE e.user.id = :userId ORDER BY e.createdAt DESC")
    List<MultisportEvent> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT DISTINCT e FROM MultisportEvent e JOIN e.segments s WHERE s.activity.id = :activityId")
    java.util.Optional<MultisportEvent> findByActivityId(@Param("activityId") Long activityId);
}
