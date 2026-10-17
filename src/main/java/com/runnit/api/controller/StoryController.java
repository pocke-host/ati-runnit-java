// ========== StoryController.java ==========
package com.runnit.api.controller;

import com.runnit.api.dto.StoryCreateDTO;
import com.runnit.api.dto.StoryDTO;
import com.runnit.api.dto.StoryUserGroupDTO;
import com.runnit.api.model.User;
import com.runnit.api.service.StoryService;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private static final Logger log = LoggerFactory.getLogger(StoryController.class);
    private final StoryService storyService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<StoryDTO> createStory(
            @RequestBody StoryCreateDTO dto,
            Authentication auth
    ) {
        String email = currentUserEmail(auth);
        log.info("Creating story for user: {}", email);
        StoryDTO story = storyService.createStory(dto, email);
        return ResponseEntity.ok(story);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<StoryUserGroupDTO>> getStoriesFeed(
            Authentication auth
    ) {
        List<StoryUserGroupDTO> stories = storyService.getStoriesFeed(currentUserEmail(auth));
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryDTO> getStory(
            @PathVariable Long id,
            Authentication auth
    ) {
        StoryDTO story = storyService.getStory(id, currentUserEmail(auth));
        return ResponseEntity.ok(story);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> markAsViewed(
            @PathVariable Long id,
            Authentication auth
    ) {
        storyService.markAsViewed(id, currentUserEmail(auth));
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(
            @PathVariable Long id,
            Authentication auth
    ) {
        storyService.deleteStory(id, currentUserEmail(auth));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<StoryDTO>> getMyStories(
            Authentication auth
    ) {
        List<StoryDTO> stories = storyService.getMyStories(currentUserEmail(auth));
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{id}/viewers")
    public ResponseEntity<List<Map<String, Object>>> getStoryViewers(
            @PathVariable Long id,
            Authentication auth
    ) {
        List<User> viewers = storyService.getStoryViewers(id, currentUserEmail(auth));
        
        List<Map<String, Object>> viewersList = viewers.stream()
            .map(user -> Map.of(
                "id", (Object) user.getId(),
                "displayName", user.getDisplayName(),
                "email", user.getEmail()
            ))
            .toList();
        
        return ResponseEntity.ok(viewersList);
    }

    private String currentUserEmail(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            throw new ResourceNotFoundException("Authentication required");
        }
        return userRepository.findById(userId)
                .map(User::getEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
