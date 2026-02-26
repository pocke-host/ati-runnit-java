// ========== MomentService.java ==========
package com.runnit.api.service;

import com.runnit.api.dto.MomentRequest;
import com.runnit.api.dto.MomentResponse;
import com.runnit.api.model.Activity;
import com.runnit.api.model.Moment;
import com.runnit.api.model.Reaction;
import com.runnit.api.model.User;
import com.runnit.api.repository.ActivityRepository;
import com.runnit.api.repository.MomentRepository;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.repository.ReactionRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MomentService {
    
    private final MomentRepository momentRepository;
    private final FollowRepository followRepository;
    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    
    @Transactional
    public Moment createMoment(Long userId, MomentRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        
        // Get User object
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if already posted today
        momentRepository.findByUserAndDayKey(user, today)
                .ifPresent(m -> {
                    throw new RuntimeException("You've already posted a moment today");
                });
        
        // Get Activity object if activityId provided
        Activity activity = null;
        if (request.getActivityId() != null) {
            activity = activityRepository.findById(request.getActivityId())
                    .orElse(null);
        }
        
        Moment moment = Moment.builder()
                .user(user)
                .activity(activity)
                .photoUrl(request.getPhotoUrl())
                .routeSnapshotUrl(request.getRouteSnapshotUrl())
                .songTitle(request.getSongTitle())
                .songArtist(request.getSongArtist())
                .songLink(request.getSongLink())
                .dayKey(today)
                .build();
        
        return momentRepository.save(moment);
    }
    
    public Page<MomentResponse> getFeed(Long userId, int page, int size) {
        List<Long> followingIds = followRepository.findFollowingUserIds(userId);
        followingIds.add(userId); // Include own moments
        
        Page<Moment> moments = momentRepository.findFeedByUserIds(followingIds, PageRequest.of(page, size));
        
        return moments.map(moment -> buildMomentResponse(moment, userId));
    }
    
    public MomentResponse getMomentById(Long id, Long currentUserId) {
        Moment moment = momentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Moment not found"));
        return buildMomentResponse(moment, currentUserId);
    }
    
    public Page<MomentResponse> getUserMoments(Long userId, Long currentUserId, int page, int size) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Page<Moment> moments = momentRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(page, size));
        return moments.map(moment -> buildMomentResponse(moment, currentUserId));
    }
    
    private MomentResponse buildMomentResponse(Moment moment, Long currentUserId) {
        User user = moment.getUser();
        
        List<Reaction> reactions = reactionRepository.findByMomentId(moment.getId());
        Map<Reaction.ReactionType, Long> reactionsByType = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getType, Collectors.counting()));
        
        Reaction.ReactionType currentUserReaction = reactions.stream()
                .filter(r -> r.getUser().getId().equals(currentUserId))
                .map(Reaction::getType)
                .findFirst()
                .orElse(null);
        
        return MomentResponse.builder()
                .id(moment.getId())
                .userId(user.getId())
                .userDisplayName(user.getDisplayName())
                // .userAvatarUrl(user.getAvatarUrl())
                .activityId(moment.getActivity() != null ? moment.getActivity().getId() : null)
                .photoUrl(moment.getPhotoUrl())
                .routeSnapshotUrl(moment.getRouteSnapshotUrl())
                .songTitle(moment.getSongTitle())
                .songArtist(moment.getSongArtist())
                .songLink(moment.getSongLink())
                .createdAt(moment.getCreatedAt())
                .reactionCount((long) reactions.size())
                .reactionsByType(reactionsByType)
                .currentUserReaction(currentUserReaction)
                .build();
    }
}