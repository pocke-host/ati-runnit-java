// ========== StoryRepository.java ==========
package com.runnit.api.repository;

import com.runnit.api.model.Story;
import com.runnit.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    
    // Find active stories by user
    List<Story> findByUserAndIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(
        User user, 
        LocalDateTime now
    );
    
    // Find all active stories for feed
    @Query("SELECT s FROM Story s WHERE s.isActive = true AND s.expiresAt > :now ORDER BY s.createdAt DESC")
    List<Story> findActiveStories(@Param("now") LocalDateTime now);
    
    // Find stories from users that the viewer follows
    @Query("""
        SELECT s FROM Story s
        WHERE s.isActive = true 
        AND s.expiresAt > :now
        AND (
            s.visibility = 'PUBLIC'
            OR (s.visibility = 'CLOSE_FRIENDS' AND :viewer MEMBER OF s.closeFriends)
        )
        AND s.user.id IN (
            SELECT f.followingUserId FROM Follow f WHERE f.followerUserId = :viewerId
        )
        ORDER BY s.createdAt DESC
    """)
    List<Story> findStoriesForUser(
        @Param("viewer") User viewer,
        @Param("viewerId") Long viewerId,
        @Param("now") LocalDateTime now
    );
    
    // Get stories grouped by user
    @Query("""
        SELECT DISTINCT s.user FROM Story s
        WHERE s.isActive = true 
        AND s.expiresAt > :now
        AND (
            s.visibility = 'PUBLIC'
            OR (s.visibility = 'CLOSE_FRIENDS' AND :viewer MEMBER OF s.closeFriends)
        )
        AND s.user.id IN (
            SELECT f.followingUserId FROM Follow f WHERE f.followerUserId = :viewerId
        )
        ORDER BY MAX(s.createdAt) DESC
    """)
    List<User> findUsersWithActiveStories(
        @Param("viewer") User viewer,
        @Param("viewerId") Long viewerId,
        @Param("now") LocalDateTime now
    );
}