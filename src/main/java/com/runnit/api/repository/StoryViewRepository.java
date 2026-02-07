// ========== StoryViewRepository.java ==========
package com.runnit.api.repository;

import com.runnit.api.model.Story;
import com.runnit.api.model.StoryView;
import com.runnit.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoryViewRepository extends JpaRepository<StoryView, Long> {
    
    Optional<StoryView> findByStoryAndUser(Story story, User user);
    
    List<StoryView> findByStoryOrderByViewedAtDesc(Story story);
    
    long countByStory(Story story);
    
    boolean existsByStoryAndUser(Story story, User user);
}