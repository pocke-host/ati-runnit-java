// ========== StoryService.java ==========
package com.runnit.api.service;

import com.runnit.api.dto.StoryCreateDTO;
import com.runnit.api.dto.StoryDTO;
import com.runnit.api.dto.StoryUserGroupDTO;
import com.runnit.api.model.Story;
import com.runnit.api.model.StoryView;
import com.runnit.api.model.User;
import com.runnit.api.repository.StoryRepository;
import com.runnit.api.repository.StoryViewRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoryService {

    private final StoryRepository storyRepository;
    private final StoryViewRepository storyViewRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Transactional
    public StoryDTO createStory(StoryCreateDTO dto, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Story story = new Story();
        story.setUser(user);
        story.setMediaUrl(dto.getMediaUrl());
        story.setMediaType(dto.getMediaType());
        story.setCaption(dto.getCaption());
        story.setVisibility(dto.getVisibility());
        story.setExpiresAt(LocalDateTime.now().plusHours(24));
        
        // Add close friends if visibility is CLOSE_FRIENDS
        if (dto.getVisibility() == Story.StoryVisibility.CLOSE_FRIENDS && 
            dto.getCloseFriendIds() != null) {
            List<User> closeFriends = userRepository.findAllById(dto.getCloseFriendIds());
            story.setCloseFriends(closeFriends);
        }
        
        Story savedStory = storyRepository.save(story);
        log.info("Created story {} for user {}", savedStory.getId(), user.getEmail());
        
        return toDTO(savedStory, user);
    }

    @Transactional(readOnly = true)
    public List<StoryUserGroupDTO> getStoriesFeed(String viewerEmail) {
        User viewer = userRepository.findByEmail(viewerEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Get all users with active stories
        List<User> usersWithStories = storyRepository.findUsersWithActiveStories(
            viewer, 
            LocalDateTime.now()
        );
        
        // Group stories by user
        Map<Long, StoryUserGroupDTO> userGroups = new LinkedHashMap<>();
        
        for (User user : usersWithStories) {
            List<Story> userStories = storyRepository
                .findByUserAndIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(
                    user, 
                    LocalDateTime.now()
                );
            
            // Filter stories viewer can see
            List<Story> viewableStories = userStories.stream()
                .filter(s -> s.canView(viewer))
                .toList();
            
            if (!viewableStories.isEmpty()) {
                StoryUserGroupDTO group = new StoryUserGroupDTO();
                group.setUserId(user.getId());
                group.setUserDisplayName(user.getDisplayName());
                group.setUserAvatar(null); // TODO: Add user avatar
                group.setHasUnviewed(viewableStories.stream()
                    .anyMatch(s -> !storyViewRepository.existsByStoryAndUser(s, viewer)));
                group.setStories(viewableStories.stream()
                    .map(s -> toDTO(s, viewer))
                    .collect(Collectors.toList()));
                
                userGroups.put(user.getId(), group);
            }
        }
        
        return new ArrayList<>(userGroups.values());
    }

    @Transactional(readOnly = true)
    public StoryDTO getStory(Long storyId, String viewerEmail) {
        User viewer = userRepository.findByEmail(viewerEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("Story not found"));
        
        if (!story.canView(viewer)) {
            throw new RuntimeException("You don't have permission to view this story");
        }
        
        return toDTO(story, viewer);
    }

    @Transactional
    public void markAsViewed(Long storyId, String viewerEmail) {
        User viewer = userRepository.findByEmail(viewerEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("Story not found"));
        
        // Don't mark as viewed if user is the story owner
        if (story.getUser().equals(viewer)) {
            return;
        }
        
        // Check if already viewed
        if (storyViewRepository.existsByStoryAndUser(story, viewer)) {
            return;
        }
        
        // Create view record
        StoryView view = new StoryView();
        view.setStory(story);
        view.setUser(viewer);
        storyViewRepository.save(view);
        
        log.info("User {} viewed story {}", viewer.getEmail(), storyId);
    }

    @Transactional
    public void deleteStory(Long storyId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("Story not found"));
        
        if (!story.getUser().equals(user)) {
            throw new RuntimeException("You can only delete your own stories");
        }
        
        // Soft delete
        story.setIsActive(false);
        storyRepository.save(story);
        
        log.info("Deleted story {} by user {}", storyId, user.getEmail());
    }

    @Transactional(readOnly = true)
    public List<StoryDTO> getMyStories(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Story> stories = storyRepository
            .findByUserAndIsActiveTrueAndExpiresAtAfterOrderByCreatedAtDesc(
                user, 
                LocalDateTime.now()
            );
        
        return stories.stream()
            .map(s -> toDTO(s, user))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<User> getStoryViewers(Long storyId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Story story = storyRepository.findById(storyId)
            .orElseThrow(() -> new RuntimeException("Story not found"));
        
        if (!story.getUser().equals(user)) {
            throw new RuntimeException("You can only view your own story viewers");
        }
        
        return storyViewRepository.findByStoryOrderByViewedAtDesc(story).stream()
            .map(StoryView::getUser)
            .collect(Collectors.toList());
    }

    // Clean up expired stories (run as scheduled job)
    @Transactional
    public void cleanupExpiredStories() {
        List<Story> expiredStories = storyRepository.findActiveStories(LocalDateTime.now());
        
        long deactivated = expiredStories.stream()
            .filter(Story::isExpired)
            .peek(s -> s.setIsActive(false))
            .count();
        
        if (deactivated > 0) {
            log.info("Deactivated {} expired stories", deactivated);
        }
    }

    private StoryDTO toDTO(Story story, User viewer) {
        StoryDTO dto = new StoryDTO();
        dto.setId(story.getId());
        dto.setUserId(story.getUser().getId());
        dto.setUserDisplayName(story.getUser().getDisplayName());
        dto.setMediaUrl(story.getMediaUrl());
        dto.setMediaType(story.getMediaType());
        dto.setCaption(story.getCaption());
        dto.setVisibility(story.getVisibility());
        dto.setCreatedAt(story.getCreatedAt());
        dto.setExpiresAt(story.getExpiresAt());
        dto.setViewCount(storyViewRepository.countByStory(story));
        dto.setHasViewed(storyViewRepository.existsByStoryAndUser(story, viewer));
        dto.setIsOwner(story.getUser().equals(viewer));
        return dto;
    }
}