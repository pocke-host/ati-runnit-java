package com.runnit.api.repository;

import com.runnit.api.model.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    Page<Activity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    long countByUserId(Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT SUM(a.distanceMeters) FROM Activity a")
    Long sumDistanceMeters();

    boolean existsByUserIdAndExternalId(Long userId, String externalId);

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Activity a WHERE a.user.id IN :userIds ORDER BY a.createdAt DESC")
    Page<Activity> findFeedByUserIds(@org.springframework.data.repository.query.Param("userIds") java.util.List<Long> userIds, Pageable pageable);
}