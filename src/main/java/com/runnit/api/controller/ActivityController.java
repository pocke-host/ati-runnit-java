package com.runnit.api.controller;

import com.runnit.api.dto.ActivityRequest;
import com.runnit.api.dto.CommentResponse;
import com.runnit.api.model.Activity;
import com.runnit.api.model.ActivityReaction;
import com.runnit.api.model.Comment;
import com.runnit.api.model.Reaction;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityReactionRepository;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.CommentRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.service.ActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final ActivityRepository activityRepository;
    private final ActivityReactionRepository activityReactionRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createActivity(
            @Valid @RequestBody ActivityRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Activity activity = activityService.createActivity(userId, request);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping
    public ResponseEntity<?> getActivities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            Authentication auth) {
        try {
            Long targetUserId = userId != null ? userId : (Long) auth.getPrincipal();
            Page<Activity> activities = activityService.getUserActivities(targetUserId, page, size);
            return ResponseEntity.ok(activities);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getActivity(@PathVariable Long id) {
        try {
            Activity activity = activityService.getActivityById(id);
            return ResponseEntity.ok(activity);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteActivity(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Activity activity = activityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Activity not found"));
            if (!activity.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            activityRepository.delete(activity);
            return ResponseEntity.ok(Map.of("message", "Activity deleted"));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        try {
            List<CommentResponse> comments = commentRepository.findByActivityIdOrderByCreatedAtAsc(id)
                    .stream().map(this::toCommentResponse).collect(Collectors.toList());
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/comments")
    @Transactional
    public ResponseEntity<?> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Activity activity = activityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Activity not found"));

            Comment comment = Comment.builder()
                    .user(user)
                    .activity(activity)
                    .content(body.get("text"))
                    .build();
            comment = commentRepository.save(comment);
            return ResponseEntity.ok(toCommentResponse(comment));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PostMapping("/{id}/reactions")
    public ResponseEntity<?> addReaction(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            Activity activity = activityRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Activity not found"));

            Reaction.ReactionType type = Reaction.ReactionType.valueOf(body.get("reactionType").toUpperCase());

            ActivityReaction reaction = activityReactionRepository
                    .findByActivityIdAndUserId(id, userId)
                    .orElse(ActivityReaction.builder().user(user).activity(activity).build());
            reaction.setType(type);
            activityReactionRepository.save(reaction);
            return ResponseEntity.ok(Map.of("reactionType", type.name()));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/{id}/reactions")
    @Transactional
    public ResponseEntity<?> removeReaction(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            activityReactionRepository.deleteByActivityIdAndUserId(id, userId);
            return ResponseEntity.ok(Map.of("message", "Reaction removed"));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private CommentResponse toCommentResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .text(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .user(CommentResponse.UserInfo.builder()
                        .id(comment.getUser().getId())
                        .displayName(comment.getUser().getDisplayName())
                        .avatarUrl(comment.getUser().getAvatarUrl())
                        .build())
                .build();
    }
}
