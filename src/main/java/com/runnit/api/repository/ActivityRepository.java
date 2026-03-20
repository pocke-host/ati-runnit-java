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

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Activity a WHERE a.user.id = :userId AND a.createdAt >= :since")
    java.util.List<Activity> findByUserIdSince(
        @org.springframework.data.repository.query.Param("userId") Long userId,
        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since
    );

    @org.springframework.data.jpa.repository.Query("SELECT a FROM Activity a JOIN FETCH a.user WHERE a.user.id IN :userIds ORDER BY a.createdAt DESC")
    Page<Activity> findFeedByUserIds(@org.springframework.data.repository.query.Param("userIds") java.util.List<Long> userIds, Pageable pageable);

    // Haversine formula: find activities within radius km of a point, public profiles only
    @org.springframework.data.jpa.repository.Query(value = """
        SELECT a.* FROM activities a
        JOIN users u ON u.id = a.user_id
        WHERE a.start_lat IS NOT NULL
          AND a.start_lng IS NOT NULL
          AND u.is_public = true
          AND (:sport IS NULL OR a.sport_type = :sport)
          AND (6371 * acos(
                cos(radians(:lat)) * cos(radians(a.start_lat)) *
                cos(radians(a.start_lng) - radians(:lng)) +
                sin(radians(:lat)) * sin(radians(a.start_lat))
              )) <= :radiusKm
        ORDER BY a.created_at DESC
        """, nativeQuery = true)
    Page<java.util.Map<String, Object>> findNearby(
        @org.springframework.data.repository.query.Param("lat") double lat,
        @org.springframework.data.repository.query.Param("lng") double lng,
        @org.springframework.data.repository.query.Param("radiusKm") double radiusKm,
        @org.springframework.data.repository.query.Param("sport") String sport,
        Pageable pageable
    );
}