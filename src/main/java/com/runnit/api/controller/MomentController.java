package com.runnit.api.controller;

import com.runnit.api.dto.CommentResponse;
import com.runnit.api.dto.MomentRequest;
import com.runnit.api.dto.MomentResponse;
import com.runnit.api.model.Comment;
import com.runnit.api.model.Moment;
import com.runnit.api.model.User;
import com.runnit.api.repository.CommentRepository;
import com.runnit.api.repository.MomentRepository;
import com.runnit.api.repository.UserRepository;
import com.runnit.api.service.MomentService;
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
@RequestMapping("/api/moments")
@RequiredArgsConstructor
public class MomentController {

    private final MomentService momentService;
    private final MomentRepository momentRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createMoment(
            @Valid @RequestBody MomentRequest request,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Moment moment = momentService.createMoment(userId, request);
            return ResponseEntity.ok(moment);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/feed")
    public ResponseEntity<?> getFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Page<MomentResponse> feed = momentService.getFeed(userId, page, size);
            return ResponseEntity.ok(feed);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMoment(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            MomentResponse moment = momentService.getMomentById(id, userId);
            return ResponseEntity.ok(moment);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteMoment(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Moment moment = momentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Moment not found"));
            if (!moment.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            momentRepository.delete(moment);
            return ResponseEntity.ok(Map.of("message", "Moment deleted"));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        try {
            List<CommentResponse> comments = commentRepository.findByMomentIdOrderByCreatedAtAsc(id)
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
            Moment moment = momentRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Moment not found"));
            Comment comment = Comment.builder()
                    .user(user)
                    .moment(moment)
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserMoments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        try {
            Long currentUserId = (Long) auth.getPrincipal();
            Page<MomentResponse> moments = momentService.getUserMoments(userId, currentUserId, page, size);
            return ResponseEntity.ok(moments);
        } catch (Exception e) {
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
