// ========== StoryController.java ==========
package com.runnit.api.controller;

import com.runnit.api.dto.StoryCreateDTO;
import com.runnit.api.dto.StoryDTO;
import com.runnit.api.dto.StoryUserGroupDTO;
import com.runnit.api.model.User;
import com.runnit.api.service.StoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/stories")
@RequiredArgsConstructor
public class StoryController {

    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<StoryDTO> createStory(
            @RequestBody StoryCreateDTO dto,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        log.info("Creating story for user: {}", userDetails.getUsername());
        StoryDTO story = storyService.createStory(dto, userDetails.getUsername());
        return ResponseEntity.ok(story);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<StoryUserGroupDTO>> getStoriesFeed(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<StoryUserGroupDTO> stories = storyService.getStoriesFeed(userDetails.getUsername());
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoryDTO> getStory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        StoryDTO story = storyService.getStory(id, userDetails.getUsername());
        return ResponseEntity.ok(story);
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> markAsViewed(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        storyService.markAsViewed(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        storyService.deleteStory(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my")
    public ResponseEntity<List<StoryDTO>> getMyStories(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<StoryDTO> stories = storyService.getMyStories(userDetails.getUsername());
        return ResponseEntity.ok(stories);
    }

    @GetMapping("/{id}/viewers")
    public ResponseEntity<List<Map<String, Object>>> getStoryViewers(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        List<User> viewers = storyService.getStoryViewers(id, userDetails.getUsername());
        
        List<Map<String, Object>> viewersList = viewers.stream()
            .map(user -> Map.of(
                "id", (Object) user.getId(),
                "displayName", user.getDisplayName(),
                "email", user.getEmail()
            ))
            .toList();
        
        return ResponseEntity.ok(viewersList);
    }
}