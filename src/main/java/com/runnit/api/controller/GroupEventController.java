package com.runnit.api.controller;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.exception.ForbiddenException;
import com.runnit.api.exception.ResourceNotFoundException;
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
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            String title = (String) body.get("title");
            if (title == null || title.isBlank()) {
                throw new BadRequestException("title is required");
            }
            String eventDatetimeRaw = (String) body.get("eventDatetime");
            if (eventDatetimeRaw == null || eventDatetimeRaw.isBlank()) {
                throw new BadRequestException("eventDatetime is required");
            }

            GroupEvent event = new GroupEvent();
            event.setCreator(creator);
            event.setTitle(title);
            event.setSportType((String) body.getOrDefault("sportType", "RUN"));
            event.setEventDatetime(LocalDateTime.parse(eventDatetimeRaw));
            event.setLocationName((String) body.get("locationName"));
            event.setDescription((String) body.get("description"));
            if (body.get("latitude") != null)
                event.setLatitude(((Number) body.get("latitude")).doubleValue());
            if (body.get("longitude") != null)
                event.setLongitude(((Number) body.get("longitude")).doubleValue());
            if (body.get("city") != null)
                event.setCity((String) body.get("city"));
            if (body.get("isPublic") != null)
                event.setPublic(Boolean.TRUE.equals(body.get("isPublic")));
            if (body.get("maxAttendees") != null)
                event.setMaxAttendees(((Number) body.get("maxAttendees")).intValue());

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
        } catch (ResourceNotFoundException | BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
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
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/group-events/discover?city=CITY
     * Returns upcoming public events in the next 30 days sorted by date ascending.
     * If city param is omitted, returns all upcoming public events regardless of city.
     */
    @GetMapping("/discover")
    @Transactional(readOnly = true)
    public ResponseEntity<?> discoverEvents(
            @RequestParam(required = false) String city,
            Authentication auth) {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime ceiling = now.plusDays(30);

            List<GroupEvent> events;
            if (city != null && !city.isBlank()) {
                events = eventRepository
                        .findByCityIgnoreCaseAndEventDatetimeAfterAndIsPublicTrueOrderByEventDatetimeAsc(city, now);
            } else {
                events = eventRepository
                        .findByEventDatetimeAfterAndIsPublicTrueOrderByEventDatetimeAsc(now);
            }

            // Cap at 30 days window
            events = events.stream()
                    .filter(e -> e.getEventDatetime().isBefore(ceiling))
                    .collect(Collectors.toList());

            // Resolve currentUserId for RSVP status — may be null if endpoint called without auth
            Long currentUserId = (auth != null) ? (Long) auth.getPrincipal() : null;

            List<Map<String, Object>> result = events.stream()
                    .map(e -> toMap(e, currentUserId))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET /api/group-events/{id}
     * Returns detail for a single event. Public events are readable without auth;
     * private events require the requesting user to be the creator or an invitee.
     */
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> getEvent(@PathVariable Long id, Authentication auth) {
        try {
            GroupEvent event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

            Long currentUserId = (auth != null) ? (Long) auth.getPrincipal() : null;

            if (!event.isPublic()) {
                if (currentUserId == null) {
                    return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
                }
                boolean isCreator = event.getCreator().getId().equals(currentUserId);
                boolean isInvitee = inviteRepository.findByEventIdAndInviteeId(id, currentUserId).isPresent();
                if (!isCreator && !isInvitee) {
                    throw new ForbiddenException("Access denied to private event");
                }
            }

            return ResponseEntity.ok(toMap(event, currentUserId));
        } catch (ResourceNotFoundException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/group-events/{id}/rsvp
     * Body: { "status": "GOING" | "INTERESTED" | "NOT_GOING" }
     * Any authenticated user can RSVP to a public event. For private events, the user
     * must already have an invite. If an invite record already exists for this user,
     * its status is updated; otherwise a new invite record is created.
     */
    @PostMapping("/{id}/rsvp")
    @Transactional
    public ResponseEntity<?> rsvpToEvent(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();

            GroupEvent event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

            String status = (String) body.get("status");
            if (status == null || status.isBlank()) {
                throw new BadRequestException("status is required");
            }

            List<String> validStatuses = List.of("GOING", "INTERESTED", "NOT_GOING");
            if (!validStatuses.contains(status)) {
                throw new BadRequestException("status must be one of: GOING, INTERESTED, NOT_GOING");
            }

            if (!event.isPublic()) {
                // Private event: user must already have an invite
                inviteRepository.findByEventIdAndInviteeId(id, userId)
                        .orElseThrow(() -> new ForbiddenException("You are not invited to this private event"));
            }

            // Find existing invite or create a new one
            GroupEventInvite invite = inviteRepository.findByEventIdAndInviteeId(id, userId)
                    .orElseGet(() -> {
                        User invitee = userRepository.findById(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                        GroupEventInvite newInvite = new GroupEventInvite();
                        newInvite.setEventId(id);
                        newInvite.setInvitee(invitee);
                        return newInvite;
                    });

            invite.setStatus(status);
            inviteRepository.save(invite);

            return ResponseEntity.ok(Map.of("id", invite.getId(), "eventId", id, "status", invite.getStatus()));
        } catch (ResourceNotFoundException | BadRequestException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * PATCH /api/group-events/invites/{id}
     * Original invite-based RSVP — updates an existing invite record by its own ID.
     * Only the invitee may update their own invite.
     */
    @PatchMapping("/invites/{id}")
    public ResponseEntity<?> updateInvite(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            GroupEventInvite invite = inviteRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Invite not found"));

            if (!invite.getInvitee().getId().equals(userId)) {
                throw new ForbiddenException("Not authorized to update this invite");
            }

            String status = (String) body.get("status");
            if (status == null || status.isBlank()) {
                throw new BadRequestException("status is required");
            }
            invite.setStatus(status);
            inviteRepository.save(invite);
            return ResponseEntity.ok(Map.of("id", invite.getId(), "status", invite.getStatus()));
        } catch (ResourceNotFoundException | BadRequestException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> deleteEvent(@PathVariable Long id, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            GroupEvent event = eventRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            if (!event.getCreator().getId().equals(userId)) {
                throw new ForbiddenException("Not authorized to delete this event");
            }

            inviteRepository.deleteAll(inviteRepository.findByEventId(id));
            eventRepository.delete(event);
            return ResponseEntity.ok(Map.of("deleted", true));
        } catch (ResourceNotFoundException | ForbiddenException e) {
            throw e;
        } catch (Exception e) {
            log.error("{} failed: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    private Map<String, Object> toMap(GroupEvent event, Long currentUserId) {
        List<GroupEventInvite> invites = inviteRepository.findByEventId(event.getId());

        long attendeeCount = invites.stream()
                .filter(i -> "GOING".equals(i.getStatus()) || "ACCEPTED".equals(i.getStatus()))
                .count();

        GroupEventInvite myInvite = (currentUserId != null)
                ? invites.stream()
                        .filter(i -> i.getInvitee().getId().equals(currentUserId))
                        .findFirst().orElse(null)
                : null;

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
        result.put("latitude", event.getLatitude());
        result.put("longitude", event.getLongitude());
        result.put("city", event.getCity());
        result.put("isPublic", event.isPublic());
        result.put("maxAttendees", event.getMaxAttendees());
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
