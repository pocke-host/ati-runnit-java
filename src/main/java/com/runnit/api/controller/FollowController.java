// ========== FollowController.java ==========
package com.runnit.api.controller;

import com.runnit.api.dto.UserResponse;
import com.runnit.api.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {
    
    private final FollowService followService;
    
    @PostMapping("/{userId}")
    public ResponseEntity<?> follow(@PathVariable Long userId, Authentication auth) {
        try {
            Long followerId = (Long) auth.getPrincipal();
            followService.followUser(followerId, userId);
            return ResponseEntity.ok(Map.of("message", "Successfully followed user"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> unfollow(@PathVariable Long userId, Authentication auth) {
        try {
            Long followerId = (Long) auth.getPrincipal();
            followService.unfollowUser(followerId, userId);
            return ResponseEntity.ok(Map.of("message", "Successfully unfollowed user"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/{userId}/followers")
    public ResponseEntity<?> getFollowers(@PathVariable Long userId) {
        try {
            List<UserResponse> followers = followService.getFollowers(userId);
            return ResponseEntity.ok(followers);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/{userId}/following")
    public ResponseEntity<?> getFollowing(@PathVariable Long userId) {
        try {
            List<UserResponse> following = followService.getFollowing(userId);
            return ResponseEntity.ok(following);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
    
    @GetMapping("/following")
    public ResponseEntity<?> getMyFollowing(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<UserResponse> following = followService.getFollowing(userId);
            return ResponseEntity.ok(following);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{userId}/is-following")
    public ResponseEntity<?> isFollowing(@PathVariable Long userId, Authentication auth) {
        try {
            Long followerId = (Long) auth.getPrincipal();
            boolean isFollowing = followService.isFollowing(followerId, userId);
            return ResponseEntity.ok(Map.of("isFollowing", isFollowing));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
