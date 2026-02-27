package com.runnit.api.service;

import com.runnit.api.dto.EmergencyContactDTO;
import com.runnit.api.dto.LiveLocationDTO;
import com.runnit.api.dto.SosAlertRequest;
import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SafetyService {

    private final EmergencyContactRepository contactRepository;
    private final LiveLocationShareRepository locationShareRepository;
    private final SosAlertRepository sosAlertRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;

    // ─── Emergency Contacts ──────────────────────────────────────────────────

    @Transactional
    public EmergencyContactDTO addContact(Long userId, EmergencyContactDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        EmergencyContact contact = EmergencyContact.builder()
                .user(user)
                .name(dto.getName())
                .phone(dto.getPhone())
                .email(dto.getEmail())
                .relationship(dto.getRelationship())
                .build();

        contact = contactRepository.save(contact);
        return toContactDTO(contact);
    }

    public List<EmergencyContactDTO> getContacts(Long userId) {
        return contactRepository.findByUserId(userId)
                .stream().map(this::toContactDTO).collect(Collectors.toList());
    }

    @Transactional
    public EmergencyContactDTO updateContact(Long contactId, Long userId, EmergencyContactDTO dto) {
        EmergencyContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        if (!contact.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }
        contact.setName(dto.getName());
        contact.setPhone(dto.getPhone());
        contact.setEmail(dto.getEmail());
        contact.setRelationship(dto.getRelationship());
        contact = contactRepository.save(contact);
        return toContactDTO(contact);
    }

    @Transactional
    public void deleteContact(Long contactId, Long userId) {
        EmergencyContact contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new RuntimeException("Contact not found"));
        if (!contact.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }
        contactRepository.delete(contact);
    }

    // ─── Live Location Sharing ───────────────────────────────────────────────

    @Transactional
    public LiveLocationDTO startLocationShare(Long userId, Long activityId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Activity activity = activityId != null
                ? activityRepository.findById(activityId).orElse(null)
                : null;

        // Stop any existing active share for this activity
        if (activityId != null) {
            locationShareRepository.findByUserIdAndIsActiveTrueAndActivityId(userId, activityId)
                    .ifPresent(share -> {
                        share.setActive(false);
                        locationShareRepository.save(share);
                    });
        }

        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 12);

        LiveLocationShare share = LiveLocationShare.builder()
                .user(user)
                .activity(activity)
                .isActive(true)
                .shareToken(token)
                .build();

        share = locationShareRepository.save(share);
        return toLocationDTO(share);
    }

    @Transactional
    public LiveLocationDTO updateLocation(String shareToken, BigDecimal lat, BigDecimal lng) {
        LiveLocationShare share = locationShareRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new RuntimeException("Share session not found"));
        if (!share.isActive()) {
            throw new RuntimeException("Share session is no longer active");
        }
        share.setLastLatitude(lat);
        share.setLastLongitude(lng);
        share.setLastUpdated(Instant.now());
        share = locationShareRepository.save(share);
        return toLocationDTO(share);
    }

    @Transactional
    public void stopLocationShare(Long userId, String shareToken) {
        LiveLocationShare share = locationShareRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new RuntimeException("Share session not found"));
        if (!share.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }
        share.setActive(false);
        locationShareRepository.save(share);
    }

    public LiveLocationDTO getLiveLocation(String shareToken) {
        LiveLocationShare share = locationShareRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new RuntimeException("Share session not found"));
        return toLocationDTO(share);
    }

    // ─── SOS / Panic Button ──────────────────────────────────────────────────

    @Transactional
    public SosAlert triggerSOS(Long userId, SosAlertRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SosAlert alert = SosAlert.builder()
                .user(user)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .message(request.getMessage())
                .status("ACTIVE")
                .build();

        alert = sosAlertRepository.save(alert);

        // In production: notify emergency contacts via SMS/email/push
        notifyEmergencyContacts(userId, alert);

        return alert;
    }

    @Transactional
    public SosAlert resolveSOS(Long alertId, Long userId) {
        SosAlert alert = sosAlertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("SOS alert not found"));
        if (!alert.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(Instant.now());
        return sosAlertRepository.save(alert);
    }

    public List<SosAlert> getMySosAlerts(Long userId) {
        return sosAlertRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // ─── Private helpers ────────────────────────────────────────────────────

    private void notifyEmergencyContacts(Long userId, SosAlert alert) {
        // TODO: Integrate with SMS provider (Twilio) or email service
        // For now, contacts are stored and notification is a no-op placeholder
        List<EmergencyContact> contacts = contactRepository.findByUserId(userId);
        // contacts.forEach(c -> smsService.send(c.getPhone(), buildSosMessage(alert)));
    }

    private EmergencyContactDTO toContactDTO(EmergencyContact c) {
        return EmergencyContactDTO.builder()
                .id(c.getId())
                .name(c.getName())
                .phone(c.getPhone())
                .email(c.getEmail())
                .relationship(c.getRelationship())
                .createdAt(c.getCreatedAt())
                .build();
    }

    private LiveLocationDTO toLocationDTO(LiveLocationShare share) {
        return LiveLocationDTO.builder()
                .id(share.getId())
                .userId(share.getUser().getId())
                .userDisplayName(share.getUser().getDisplayName())
                .activityId(share.getActivity() != null ? share.getActivity().getId() : null)
                .lastLatitude(share.getLastLatitude())
                .lastLongitude(share.getLastLongitude())
                .lastUpdated(share.getLastUpdated())
                .isActive(share.isActive())
                .shareToken(share.getShareToken())
                .shareUrl("/live/" + share.getShareToken())
                .createdAt(share.getCreatedAt())
                .build();
    }
}
