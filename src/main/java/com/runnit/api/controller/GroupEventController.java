package com.runnit.api.controller;

import com.runnit.api.model.GroupEvent;
import com.runnit.api.model.GroupEventInvite;
import com.runnit.api.model.User;
import com.runnit.api.repository.GroupEventInviteRepository;
import com.runnit.api.repository.GroupEventRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/group-events")
@RequiredArgsConstructor
public class GroupEventController {

    private final GroupEventRepository eventRepository;
    private final GroupEventInviteRepository inviteRepository;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> createEvent(@RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            User creator = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            GroupEvent event = new GroupEvent();
            event.setCreator(creator);
            event.setTitle((String) body.get("title"));
            event.setSportType((String) body.getOrDefault("sportType", "RUN"));
            event.setEventDatetime(LocalDateTime.parse((String) body.get("eventDatetime")));
            event.setLocationName((String) body.get("locationName"));
            event.setDescription((String) body.get("description"));

            GroupEvent saved = eventRepository.save(event);

            @SuppressWarnings("unchecked")
            List<Number> inviteeIds = (List<Number>) body.get("inviteeIds");
            if (inviteeIds != null) {
                for (Number inviteeId : inviteeIds) {
                    userRepository.findById(inviteeId.longValue()).ifPresent(invitee -> {
                        GroupEventInvite invite = new GroupEventInvite();
                        invite.setEventId(saved.getId());
                        invite.setInvitee(invitee);
                        invite.setStatus("PENDING");
                        inviteRepository.save(invite);
                    });
                }
            }

            return ResponseEntity.ok(toMap(saved, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyEvents(Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();

            // Events created by user
            List<GroupEvent> createdEvents = eventRepository.findByCreatorIdOrderByEventDatetimeAsc(userId);

            // Events where user is invited (not declined)
            List<GroupEventInvite> myInvites = inviteRepository.findByInviteeIdAndStatusNot(userId, "DECLINED");
            Set<Long> createdIds = createdEvents.stream().map(GroupEvent::getId).collect(Collectors.toSet());

            List<GroupEvent> invitedEvents = myInvites.stream()
                    .map(i -> eventRepository.findById(i.getEventId()).orElse(null))
                    .filter(e -> e != null && !createdIds.contains(e.getId()))
                    .collect(Collectors.toList());

            List<GroupEvent> allEvents = new ArrayList<>();
            allEvents.addAll(createdEvents);
            allEvents.addAll(invitedEvents);
            allEvents.sort(Comparator.comparing(GroupEvent::getEventDatetime));

            List<Map<String, Object>> result = allEvents.stream()
                    .map(e -> toMap(e, userId))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/invites/{id}")
    public ResponseEntity<?> rsvp(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            GroupEventInvite invite = inviteRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Invite not found"));

            if (!invite.getInvitee().getId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));

            String status = (String) body.get("status");
            invite.setStatus(status);
            inviteRepository.save(invite);
            return ResponseEntity.ok(Map.of("id", invite.getId(), "status", invite.getStatus()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            GroupEvent event = eventRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Event not found"));
            if (!event.getCreator().getId().equals(userId))
                return ResponseEntity.status(403).body(Map.of("error", "Not authorized"));

            List<GroupEventInvite> invites = inviteRepository.findByEventId(id);
            inviteRepository.deleteAll(invites);
            eventRepository.delete(event);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (Exception e) {
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
            m.put("displayName", i.getInvitee().getFirstName() + " " + i.getInvitee().getLastName());
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
        result.put("creatorName", event.getCreator().getFirstName() + " " + event.getCreator().getLastName());
        result.put("attendeeCount", attendeeCount);
        result.put("myInviteId", myInvite != null ? myInvite.getId() : null);
        result.put("myRsvpStatus", myInvite != null ? myInvite.getStatus() : null);
        result.put("invitees", inviteeList);
        result.put("createdAt", event.getCreatedAt() != null ? event.getCreatedAt().toString() : null);
        return result;
    }
}
