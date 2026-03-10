package com.runnit.api.controller;

import com.runnit.api.model.DirectMessage;
import com.runnit.api.model.User;
import com.runnit.api.repository.DirectMessageRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dms")
@RequiredArgsConstructor
public class DmController {

    private final DirectMessageRepository dmRepository;
    private final UserRepository userRepository;

    @GetMapping("/conversations")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getConversations(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<DirectMessage> latest = dmRepository.findLatestMessagePerConversation(userId);

            List<Long> partnerIds = latest.stream()
                    .map(m -> m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId())
                    .collect(Collectors.toList());

            Map<Long, User> userMap = userRepository.findAllById(partnerIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));

            List<Map<String, Object>> conversations = latest.stream().map(m -> {
                Long partnerId = m.getSenderId().equals(userId) ? m.getReceiverId() : m.getSenderId();
                User partner = userMap.get(partnerId);
                Map<String, Object> conv = new HashMap<>();
                conv.put("userId", partnerId);
                conv.put("displayName", partner != null ? partner.getDisplayName() : "Unknown");
                conv.put("avatarUrl", partner != null ? partner.getAvatarUrl() : null);
                conv.put("lastMessage", m.getText());
                conv.put("timestamp", m.getCreatedAt());
                conv.put("unread", !m.getSenderId().equals(userId) && !m.isRead());
                return conv;
            }).collect(Collectors.toList());

            return ResponseEntity.ok(conversations);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{partnerId}/messages")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMessages(@PathVariable Long partnerId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            List<DirectMessage> messages = dmRepository.findConversation(userId, partnerId);
            List<Map<String, Object>> result = messages.stream().map(m -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", m.getId());
                map.put("userId", m.getSenderId());
                map.put("text", m.getText());
                map.put("createdAt", m.getCreatedAt());
                map.put("read", m.isRead());
                return map;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{partnerId}")
    @Transactional
    public ResponseEntity<?> sendMessage(
            @PathVariable Long partnerId,
            @RequestBody Map<String, String> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            String text = body.get("text");
            if (text == null || text.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Message text is required"));
            }
            if (!userRepository.existsById(partnerId)) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            DirectMessage msg = new DirectMessage();
            msg.setSenderId(userId);
            msg.setReceiverId(partnerId);
            msg.setText(text);
            msg = dmRepository.save(msg);

            return ResponseEntity.ok(Map.of(
                    "id", msg.getId(),
                    "userId", msg.getSenderId(),
                    "text", msg.getText(),
                    "createdAt", msg.getCreatedAt(),
                    "read", msg.isRead()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{partnerId}/read")
    @Transactional
    public ResponseEntity<?> markRead(@PathVariable Long partnerId, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            // Mark messages sent by partner to current user as read
            dmRepository.markConversationRead(partnerId, userId);
            return ResponseEntity.ok(Map.of("message", "Conversation marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
