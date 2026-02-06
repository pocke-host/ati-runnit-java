// ========== MomentService.java ==========
package com.runnit.api.service;

import com.runnit.api.dto.MomentRequest;
import com.runnit.api.dto.MomentResponse;
import com.runnit.api.model.Moment;
import com.runnit.api.model.Reaction;
import com.runnit.api.model.User;
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
    
    @Transactional
    public Moment createMoment(Long userId, MomentRequest request) {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        
        // Check if already posted today
        momentRepository.findByUserIdAndDayKey(userId, today)
                .ifPresent(m -> {
                    throw new RuntimeException("You've already posted a moment today");
                });
        
        Moment moment = Moment.builder()
                .userId(userId)
                .activityId(request.getActivityId())
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
        Page<Moment> moments = momentRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return moments.map(moment -> buildMomentResponse(moment, currentUserId));
    }
    
    private MomentResponse buildMomentResponse(Moment moment, Long currentUserId) {
        User user = userRepository.findById(moment.getUserId()).orElse(null);
        
        List<Reaction> reactions = reactionRepository.findByMomentId(moment.getId());
        Map<Reaction.ReactionType, Long> reactionsByType = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getType, Collectors.counting()));
        
        Reaction.ReactionType currentUserReaction = reactions.stream()
                .filter(r -> r.getUserId().equals(currentUserId))
                .map(Reaction::getType)
                .findFirst()
                .orElse(null);
        
        return MomentResponse.builder()
                .id(moment.getId())
                .userId(moment.getUserId())
                .userDisplayName(user != null ? user.getDisplayName() : "Unknown")
                .userAvatarUrl(user != null ? user.getAvatarUrl() : null)
                .activityId(moment.getActivityId())
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