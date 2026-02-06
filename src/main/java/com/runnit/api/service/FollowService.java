// ========== FollowService.java ==========
package com.runnit.api.service;

import com.runnit.api.dto.UserResponse;
import com.runnit.api.model.Follow;
import com.runnit.api.model.FollowId;
import com.runnit.api.model.User;
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
            throw new RuntimeException("Cannot follow yourself");
        }
        
        if (!userRepository.existsById(followingId)) {
            throw new RuntimeException("User not found");
        }
        
        if (followRepository.existsByFollowerUserIdAndFollowingUserId(followerId, followingId)) {
            throw new RuntimeException("Already following this user");
        }
        
        Follow follow = Follow.builder()
                .followerUserId(followerId)
                .followingUserId(followingId)
                .build();
        
        followRepository.save(follow);
    }
    
    @Transactional
    public void unfollowUser(Long followerId, Long followingId) {
        followRepository.findById(new FollowId(followerId, followingId))
                .ifPresent(followRepository::delete);
    }
    
    public List<UserResponse> getFollowers(Long userId) {
        List<Follow> follows = followRepository.findByFollowingUserId(userId);
        List<Long> followerIds = follows.stream()
                .map(Follow::getFollowerUserId)
                .collect(Collectors.toList());
        
        return userRepository.findAllById(followerIds).stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }
    
    public List<UserResponse> getFollowing(Long userId) {
        List<Follow> follows = followRepository.findByFollowerUserId(userId);
        List<Long> followingIds = follows.stream()
                .map(Follow::getFollowingUserId)
                .collect(Collectors.toList());
        
        return userRepository.findAllById(followingIds).stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }
    
    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerUserIdAndFollowingUserId(followerId, followingId);
    }
    
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .displayName(user.getDisplayName())
                .avatarUrl(user.getAvatarUrl())
                .followersCount(followRepository.countByFollowingUserId(user.getId()))
                .followingCount(followRepository.countByFollowerUserId(user.getId()))
                .build();
    }
}