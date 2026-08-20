package com.runnit.api.service;

import com.runnit.api.dto.UserResponse;
import com.runnit.api.model.Follow;
import com.runnit.api.model.User;
import com.runnit.api.exception.BadRequestException;
import com.runnit.api.exception.ConflictException;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.repository.FollowRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {
    
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    
    @Transactional
    public void followUser(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BadRequestException("Cannot follow yourself");
        }
        
        if (!userRepository.existsById(followingId)) {
            throw new ResourceNotFoundException("User not found");
        }
        
        if (followRepository.existsByFollowerUserIdAndFollowingUserId(followerId, followingId)) {
            throw new ConflictException("Already following this user");
        }
        
        Follow follow = Follow.builder()
                .followerUserId(followerId)
                .followingUserId(followingId)
                .build();
        
        followRepository.save(follow);
    }
    
    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        followRepository.deleteByFollowerUserIdAndFollowingUserId(followerId, followingId);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFollowers(Long userId) {
        List<Long> followerIds = followRepository.findByFollowingUserId(userId).stream()
                .map(Follow::getFollowerUserId)
                .collect(Collectors.toList());
        java.util.Set<Long> followingBack = followRepository.findByFollowerUserId(userId).stream()
                .map(Follow::getFollowingUserId)
                .collect(Collectors.toSet());
        return userRepository.findAllById(followerIds).stream()
                .map(u -> toUserResponse(u, followingBack.contains(u.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getFollowing(Long userId) {
        List<Long> followingIds = followRepository.findByFollowerUserId(userId).stream()
                .map(Follow::getFollowingUserId)
                .collect(Collectors.toList());
        return userRepository.findAllById(followingIds).stream()
                .map(u -> toUserResponse(u, true))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerUserIdAndFollowingUserId(followerId, followingId);
    }

    /**
     * "Athletes you may know" — same city first (the strongest signal we have without a
     * real social graph to mine), topped up with recently-joined public users if the city
     * pool comes up short or the user hasn't set a location.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getSuggestions(Long userId, int limit) {
        User me = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        java.util.Set<Long> exclude = followRepository.findByFollowerUserId(userId).stream()
                .map(Follow::getFollowingUserId)
                .collect(Collectors.toSet());
        exclude.add(userId);

        List<User> sameCity = (me.getLocation() != null && !me.getLocation().isBlank())
                ? userRepository.findByLocationIgnoreCase(me.getLocation()).stream()
                        .filter(u -> !exclude.contains(u.getId()))
                        .collect(Collectors.toList())
                : List.of();

        List<User> result = new java.util.ArrayList<>(sameCity);
        if (result.size() < limit) {
            java.util.Set<Long> picked = result.stream().map(User::getId).collect(Collectors.toSet());
            picked.addAll(exclude);
            userRepository.findTop50ByIsPublicTrueOrderByCreatedAtDesc().stream()
                    .filter(u -> !picked.contains(u.getId()))
                    .limit(limit - result.size())
                    .forEach(result::add);
        }

        return result.stream()
                .limit(limit)
                .map(u -> UserResponse.builder()
                        .id(u.getId())
                        .displayName(u.getDisplayName())
                        .avatarUrl(u.getAvatarUrl())
                        .sport(u.getSport())
                        .location(u.getLocation())
                        .isFollowing(false)
                        .build())
                .collect(Collectors.toList());
    }

    private UserResponse toUserResponse(User user, boolean isFollowing) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .sport(user.getSport())
                .primarySport(user.getSport())
                .isFollowing(isFollowing)
                .build();
    }
}