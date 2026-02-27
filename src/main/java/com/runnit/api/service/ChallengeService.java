package com.runnit.api.service;

import com.runnit.api.dto.ChallengeRequest;
import com.runnit.api.dto.ChallengeResponse;
import com.runnit.api.dto.LeaderboardEntryDTO;
import com.runnit.api.model.Challenge;
import com.runnit.api.model.ChallengeParticipant;
import com.runnit.api.model.User;
import com.runnit.api.repository.ChallengeParticipantRepository;
import com.runnit.api.repository.ChallengeRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChallengeResponse createChallenge(Long userId, ChallengeRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Challenge challenge = Challenge.builder()
                .creator(creator)
                .title(request.getTitle())
                .description(request.getDescription())
                .sportType(request.getSportType())
                .challengeType(request.getChallengeType())
                .goalValue(request.getGoalValue())
                .goalUnit(request.getGoalUnit())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isGroup(request.isGroup())
                .maxParticipants(request.getMaxParticipants())
                .isPublic(request.isPublic())
                .charityName(request.getCharityName())
                .charityUrl(request.getCharityUrl())
                .coverImageUrl(request.getCoverImageUrl())
                .build();

        challenge = challengeRepository.save(challenge);
        return toChallengeResponse(challenge, userId);
    }

    public Page<ChallengeResponse> getActiveChallenges(Long currentUserId, int page, int size) {
        return challengeRepository.findActiveChallenges(LocalDate.now(), PageRequest.of(page, size))
                .map(c -> toChallengeResponse(c, currentUserId));
    }

    public Page<ChallengeResponse> getAllPublicChallenges(Long currentUserId, int page, int size) {
        return challengeRepository.findByIsPublicTrue(PageRequest.of(page, size))
                .map(c -> toChallengeResponse(c, currentUserId));
    }

    public List<ChallengeResponse> getMyChallenges(Long userId) {
        return challengeRepository.findChallengesForUser(userId)
                .stream().map(c -> toChallengeResponse(c, userId)).collect(Collectors.toList());
    }

    public ChallengeResponse getChallengeById(Long challengeId, Long currentUserId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));
        return toChallengeResponse(challenge, currentUserId);
    }

    @Transactional
    public ChallengeResponse joinChallenge(Long challengeId, Long userId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        if (participantRepository.existsByChallengeIdAndUserId(challengeId, userId)) {
            throw new RuntimeException("Already joined this challenge");
        }

        if (challenge.getMaxParticipants() != null) {
            long count = participantRepository.countByChallengeId(challengeId);
            if (count >= challenge.getMaxParticipants()) {
                throw new RuntimeException("Challenge is full");
            }
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChallengeParticipant participant = ChallengeParticipant.builder()
                .challenge(challenge)
                .user(user)
                .currentValue(0)
                .isCompleted(false)
                .build();
        participantRepository.save(participant);

        return toChallengeResponse(challenge, userId);
    }

    @Transactional
    public ChallengeResponse leaveChallenge(Long challengeId, Long userId) {
        ChallengeParticipant participant = participantRepository.findByChallengeIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new RuntimeException("Not a participant"));
        participantRepository.delete(participant);
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));
        return toChallengeResponse(challenge, userId);
    }

    @Transactional
    public ChallengeResponse updateProgress(Long challengeId, Long userId, double newValue) {
        ChallengeParticipant participant = participantRepository.findByChallengeIdAndUserId(challengeId, userId)
                .orElseThrow(() -> new RuntimeException("Not a participant"));
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new RuntimeException("Challenge not found"));

        participant.setCurrentValue(newValue);
        if (newValue >= challenge.getGoalValue() && !participant.isCompleted()) {
            participant.setCompleted(true);
            participant.setCompletedAt(Instant.now());
        }
        participantRepository.save(participant);
        return toChallengeResponse(challenge, userId);
    }

    public List<LeaderboardEntryDTO> getLeaderboard(Long challengeId) {
        List<ChallengeParticipant> participants = participantRepository.findLeaderboard(challengeId);
        AtomicInteger rank = new AtomicInteger(1);
        return participants.stream().map(p -> LeaderboardEntryDTO.builder()
                .rank(rank.getAndIncrement())
                .userId(p.getUser().getId())
                .displayName(p.getUser().getDisplayName())
                .avatarUrl(p.getUser().getAvatarUrl())
                .currentValue(p.getCurrentValue())
                .isCompleted(p.isCompleted())
                .build()).collect(Collectors.toList());
    }

    private ChallengeResponse toChallengeResponse(Challenge c, Long currentUserId) {
        long participantCount = participantRepository.countByChallengeId(c.getId());
        ChallengeParticipant myParticipation = participantRepository.findByChallengeIdAndUserId(c.getId(), currentUserId).orElse(null);

        return ChallengeResponse.builder()
                .id(c.getId())
                .creatorId(c.getCreator().getId())
                .creatorDisplayName(c.getCreator().getDisplayName())
                .title(c.getTitle())
                .description(c.getDescription())
                .sportType(c.getSportType())
                .challengeType(c.getChallengeType())
                .goalValue(c.getGoalValue())
                .goalUnit(c.getGoalUnit())
                .startDate(c.getStartDate())
                .endDate(c.getEndDate())
                .isGroup(c.isGroup())
                .maxParticipants(c.getMaxParticipants())
                .isPublic(c.isPublic())
                .charityName(c.getCharityName())
                .charityUrl(c.getCharityUrl())
                .coverImageUrl(c.getCoverImageUrl())
                .participantCount(participantCount)
                .isJoined(myParticipation != null)
                .currentUserProgress(myParticipation != null ? myParticipation.getCurrentValue() : null)
                .currentUserCompleted(myParticipation != null && myParticipation.isCompleted())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
