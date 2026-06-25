package com.runnit.api.repository;

import com.runnit.api.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findAllByOrderByCreatedAtDesc();

    @Query("SELECT c FROM Club c JOIN ClubMember cm ON c.id = cm.clubId WHERE cm.userId = :userId")
    List<Club> findByMemberUserId(Long userId);

    List<Club> findByCityIgnoreCaseOrderByMemberCountDesc(String city);

    List<Club> findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrSportContainingIgnoreCase(
            String name, String city, String sport);

    @Query(value = """
            SELECT * FROM clubs
            WHERE latitude IS NOT NULL AND longitude IS NOT NULL
            AND (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) *
                 cos(radians(longitude) - radians(:lng)) +
                 sin(radians(:lat)) * sin(radians(latitude)))) < :radiusKm
            ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) *
                 cos(radians(longitude) - radians(:lng)) +
                 sin(radians(:lat)) * sin(radians(latitude))))
            """, nativeQuery = true)
    List<Club> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radiusKm") double radiusKm);
}
