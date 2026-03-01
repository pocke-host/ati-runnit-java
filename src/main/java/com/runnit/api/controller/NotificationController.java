package com.runnit.api.controller;

import com.runnit.api.model.Notification;
import com.runnit.api.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<?> getNotifications(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<Map<String, Object>> notifications = notificationRepository
                    .findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .map(this::toMap)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));
            if (!notification.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }
            notification.setRead(true);
            notificationRepository.save(notification);
            return ResponseEntity.ok(toMap(notification));
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @PatchMapping("/read-all")
    @Transactional
    public ResponseEntity<?> markAllRead(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            notificationRepository.markAllReadByUserId(userId);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    private Map<String, Object> toMap(Notification n) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", n.getId());
        map.put("type", n.getType());
        map.put("message", n.getMessage());
        map.put("read", n.isRead());
        map.put("createdAt", n.getCreatedAt());
        map.put("referenceId", n.getReferenceId());
        map.put("referenceType", n.getReferenceType());
        if (n.getActor() != null) {
            map.put("actor", Map.of(
                "id", n.getActor().getId(),
                "displayName", n.getActor().getDisplayName()
            ));
        }
        return map;
    }
}
