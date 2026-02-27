package com.runnit.api.service;

import com.runnit.api.dto.MentorshipMatchDTO;
import com.runnit.api.dto.MentorshipRequest;
import com.runnit.api.dto.UserResponse;
import com.runnit.api.model.MentorshipMatch;
import com.runnit.api.model.User;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.repository.MentorshipMatchRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MentorshipService {

    private final MentorshipMatchRepository matchRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    // ─── Request mentorship ──────────────────────────────────────────────────

    @Transactional
    public MentorshipMatchDTO requestMentorship(Long menteeId, MentorshipRequest request) {
        if (menteeId.equals(request.getMentorId())) {
            throw new RuntimeException("Cannot request mentorship from yourself");
        }
        if (matchRepository.existsByMentorIdAndMenteeId(request.getMentorId(), menteeId)) {
            throw new RuntimeException("Mentorship request already exists");
        }

        User mentor = userRepository.findById(request.getMentorId())
                .orElseThrow(() -> new RuntimeException("Mentor not found"));
        User mentee = userRepository.findById(menteeId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MentorshipMatch match = MentorshipMatch.builder()
                .mentor(mentor)
                .mentee(mentee)
                .sportType(request.getSportType())
                .status("PENDING")
                .menteeGoals(request.getMenteeGoals())
                .build();

        match = matchRepository.save(match);
        return toDTO(match);
    }

    @Transactional
    public MentorshipMatchDTO respondToRequest(Long matchId, Long mentorId, String status, String mentorNotes) {
        MentorshipMatch match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Mentorship match not found"));
        if (!match.getMentor().getId().equals(mentorId)) {
            throw new RuntimeException("Not authorized");
        }
        match.setStatus(status); // ACTIVE or DECLINED
        match.setMentorNotes(mentorNotes);
        match = matchRepository.save(match);
        return toDTO(match);
    }

    public List<MentorshipMatchDTO> getMyMentorships(Long userId) {
        List<MentorshipMatch> asMentee = matchRepository.findByMenteeId(userId);
        List<MentorshipMatch> asMentor = matchRepository.findByMentorId(userId);
        asMentee.addAll(asMentor);
        return asMentee.stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<MentorshipMatchDTO> getMyMentees(Long mentorId) {
        return matchRepository.findByMentorId(mentorId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    // ─── Available mentors ───────────────────────────────────────────────────

    public Page<UserResponse> findAvailableMentors(Long userId, int page, int size) {
        return userRepository.findAvailableMentors(userId, PageRequest.of(page, size))
                .map(this::toUserResponse);
    }

    @Transactional
    public void setMentorAvailable(Long userId, boolean available) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setMentorAvailable(available);
        userRepository.save(user);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private MentorshipMatchDTO toDTO(MentorshipMatch match) {
        return MentorshipMatchDTO.builder()
                .id(match.getId())
                .mentor(toUserResponse(match.getMentor()))
                .mentee(toUserResponse(match.getMentee()))
                .sportType(match.getSportType())
                .status(match.getStatus())
                .mentorNotes(match.getMentorNotes())
                .menteeGoals(match.getMenteeGoals())
                .createdAt(match.getCreatedAt())
                .updatedAt(match.getUpdatedAt())
                .build();
    }

    private UserResponse toUserResponse(User user) {
        long followers = followRepository.countByFollowingUserId(user.getId());
        long following = followRepository.countByFollowerUserId(user.getId());
        return UserResponse.builder()
                .id(user.getId())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .bio(user.getBio())
                .role(user.getRole())
                .isVerified(user.isVerified())
                .isMentorAvailable(user.isMentorAvailable())
                .followersCount(followers)
                .followingCount(following)
                .build();
    }
}
