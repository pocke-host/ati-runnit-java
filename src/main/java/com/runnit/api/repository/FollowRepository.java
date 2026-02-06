package com.runnit.api.repository;

import com.runnit.api.model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    
    @Query("SELECT f.followingUserId FROM Follow f WHERE f.followerUserId = :userId")
    List<Long> findFollowingUserIds(@Param("userId") Long userId);
    
    List<Follow> findByFollowerUserId(Long userId);
    List<Follow> findByFollowingUserId(Long userId);
    
    boolean existsByFollowerUserIdAndFollowingUserId(Long followerId, Long followingId);
    
    long countByFollowerUserId(Long userId);
    long countByFollowingUserId(Long userId);
}