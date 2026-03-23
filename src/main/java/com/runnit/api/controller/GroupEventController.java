package com.runnit.api.controller;

import com.runnit.api.model.GroupEvent;
import com.runnit.api.model.GroupEventInvite;
import com.runnit.api.model.User;
import com.runnit.api.repository.GroupEventInviteRepository;
import com.runnit.api.repository.GroupEventRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/group-events")
@RequiredArgsConstructor
public class GroupEventController {

    private final GroupEventRepository eventRepository;
    private final GroupEventInviteRepository inviteRepository;
    private final UserRepository userRepository;

    @PostMapping
    @Transactional
    public ResponseEntity<?> createEvent(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User creator = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String title = (String) body.get("title");
            if (title == null || title.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "title is required"));
            }
            String eventDatetimeRaw = (String) body.get("eventDatetime");
            if (eventDatetimeRaw == null || eventDatetimeRaw.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "eventDatetime is required"));
            }

            GroupEvent event = new GroupEvent();
            event.setCreator(creator);
            event.setTitle(title);
            event.setSportType((String) body.getOrDefault("sportType", "RUN"));
            event.setEventDatetime(LocalDateTime.parse(eventDatetimeRaw));
            event.setLocationName((String) body.get("locationName"));
            event.setDescription((String) body.get("description"));

            GroupEvent saved = eventRepository.save(event);

            @SuppressWarnings("unchecked")
            List<Number> inviteeIds = (List<Number>) body.get("inviteeIds");
            if (inviteeIds != null && !inviteeIds.isEmpty()) {
                List<Long> ids = inviteeIds.stream().map(Number::longValue).collect(Collectors.toList());
                List<GroupEventInvite> invites = userRepository.findAllById(ids).stream()
                        .map(invitee -> {
                            GroupEventInvite invite = new GroupEventInvite();
                            invite.setEventId(saved.getId());
                            invite.setInvitee(invitee);
                            invite.setStatus("PENDING");
                            return invite;
                        }).collect(Collectors.toList());
                inviteRepository.saveAll(invites);
            }

            return ResponseEntity.ok(toMap(saved, userId));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to create group event: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getMyEvents(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();

            // Events created by user
            List<GroupEvent> createdEvents = eventRepository.findByCreatorIdOrderByEventDatetimeAsc(userId);
            Set<Long> createdIds = createdEvents.stream().map(GroupEvent::getId).collect(Collectors.toSet());

            // Events where user is invited (not declined) — batch-load events via findAllById
            List<GroupEventInvite> myInvites = inviteRepository.findByInviteeIdAndStatusNot(userId, "DECLINED");
            List<Long> invitedEventIds = myInvites.stream()
                    .map(GroupEventInvite::getEventId)
                    .filter(id -> !createdIds.contains(id))
                    .distinct()
                    .collect(Collectors.toList());
            List<GroupEvent> invitedEvents = eventRepository.findAllById(invitedEventIds);

            List<GroupEvent> allEvents = new ArrayList<>();
            allEvents.addAll(createdEvents);
            allEvents.addAll(invitedEvents);
            allEvents.sort(Comparator.comparing(GroupEvent::getEventDatetime));

            List<Map<String, Object>> result = allEvents.stream()
                    .map(e -> toMap(e, userId))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to fetch group events for user: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/invites/{id}")
    public ResponseEntity<?> rsvp(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            GroupEventInvite invite = inviteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Invite not found"));

            if (!invite.getInvitee().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }

            String status = (String) body.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "status is required"));
            }
            invite.setStatus(status);
            inviteRepository.save(invite);
            return ResponseEntity.ok(Map.of("id", invite.getId(), "status", invite.getStatus()));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to RSVP invite id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            GroupEvent event = eventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            if (!event.getCreator().getId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));
            }

            inviteRepository.deleteAll(inviteRepository.findByEventId(id));
            eventRepository.delete(event);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            log.error("Failed to delete group event id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(GroupEvent event, Long currentUserId) {
        List<GroupEventInvite> invites = inviteRepository.findByEventId(event.getId());

        long attendeeCount = invites.stream()
                .filter(i -> "ACCEPTED".equals(i.getStatus()))
                .count();

        GroupEventInvite myInvite = invites.stream()
                .filter(i -> i.getInvitee().getId().equals(currentUserId))
                .findFirst().orElse(null);

        List<Map<String, Object>> inviteeList = invites.stream().map(i -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("inviteId", i.getId());
            m.put("userId", i.getInvitee().getId());
            m.put("displayName", i.getInvitee().getDisplayName());
            m.put("status", i.getStatus());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", event.getId());
        result.put("title", event.getTitle());
        result.put("sportType", event.getSportType());
        result.put("eventDatetime", event.getEventDatetime() != null ? event.getEventDatetime().toString() : null);
        result.put("locationName", event.getLocationName());
        result.put("description", event.getDescription());
        result.put("creatorId", event.getCreator().getId());
        result.put("creatorName", event.getCreator().getDisplayName());
        result.put("attendeeCount", attendeeCount);
        result.put("myInviteId", myInvite != null ? myInvite.getId() : null);
        result.put("myRsvpStatus", myInvite != null ? myInvite.getStatus() : null);
        result.put("invitees", inviteeList);
        result.put("createdAt", event.getCreatedAt() != null ? event.getCreatedAt().toString() : null);
        return result;
    }
}
