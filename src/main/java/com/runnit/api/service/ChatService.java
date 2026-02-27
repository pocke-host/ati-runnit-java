package com.runnit.api.service;

import com.runnit.api.dto.ChatMessageDTO;
import com.runnit.api.dto.ChatRoomDTO;
import com.runnit.api.dto.CreateRoomRequest;
import com.runnit.api.dto.SendMessageRequest;
import com.runnit.api.dto.UserResponse;
import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository roomRepository;
    private final ChatRoomMemberRepository memberRepository;
    private final ChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TrainingPlanRepository trainingPlanRepository;
    private final ChallengeRepository challengeRepository;
    private final FollowRepository followRepository;

    // ─── Rooms ───────────────────────────────────────────────────────────────

    @Transactional
    public ChatRoomDTO createRoom(Long userId, CreateRoomRequest request) {
        User creator = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // For DIRECT rooms, check if one already exists
        if ("DIRECT".equals(request.getRoomType()) && request.getMemberIds() != null && request.getMemberIds().size() == 1) {
            Long otherId = request.getMemberIds().get(0);
            List<ChatRoom> existing = roomRepository.findDirectRoom(userId, otherId);
            if (!existing.isEmpty()) {
                return toRoomDTO(existing.get(0), userId);
            }
        }

        TrainingPlan trainingPlan = null;
        if (request.getTrainingPlanId() != null) {
            trainingPlan = trainingPlanRepository.findById(request.getTrainingPlanId()).orElse(null);
        }
        Challenge challenge = null;
        if (request.getChallengeId() != null) {
            challenge = challengeRepository.findById(request.getChallengeId()).orElse(null);
        }

        ChatRoom room = ChatRoom.builder()
                .name(request.getName())
                .roomType(request.getRoomType())
                .avatarUrl(request.getAvatarUrl())
                .createdBy(creator)
                .trainingPlan(trainingPlan)
                .challenge(challenge)
                .build();
        room = roomRepository.save(room);

        // Add creator as admin
        addMember(room, creator, "ADMIN");

        // Add other members
        if (request.getMemberIds() != null) {
            for (Long memberId : request.getMemberIds()) {
                userRepository.findById(memberId).ifPresent(u -> addMember(room, u, "MEMBER"));
            }
        }

        return toRoomDTO(room, userId);
    }

    public List<ChatRoomDTO> getMyRooms(Long userId) {
        return roomRepository.findRoomsForUser(userId)
                .stream().map(r -> toRoomDTO(r, userId)).collect(Collectors.toList());
    }

    public ChatRoomDTO getRoomById(Long roomId, Long userId) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("Not a member of this room");
        }
        return toRoomDTO(room, userId);
    }

    @Transactional
    public void leaveRoom(Long roomId, Long userId) {
        if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("Not a member of this room");
        }
        memberRepository.deleteByRoomIdAndUserId(roomId, userId);
    }

    // ─── Messages ────────────────────────────────────────────────────────────

    @Transactional
    public ChatMessageDTO sendMessage(Long roomId, Long userId, SendMessageRequest request) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("Not a member of this room");
        }
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Activity activity = null;
        if (request.getActivityId() != null) {
            activity = null; // resolved lazily by ActivityRepository if needed
        }

        ChatMessage message = ChatMessage.builder()
                .room(room)
                .sender(sender)
                .content(request.getContent())
                .messageType(request.getMessageType())
                .mediaUrl(request.getMediaUrl())
                .build();
        message = messageRepository.save(message);
        return toMessageDTO(message);
    }

    public Page<ChatMessageDTO> getMessages(Long roomId, Long userId, int page, int size) {
        if (!memberRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new RuntimeException("Not a member of this room");
        }
        return messageRepository.findByRoomIdOrderByCreatedAtDesc(roomId, PageRequest.of(page, size))
                .map(this::toMessageDTO);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void addMember(ChatRoom room, User user, String role) {
        if (!memberRepository.existsByRoomIdAndUserId(room.getId(), user.getId())) {
            ChatRoomMember member = ChatRoomMember.builder()
                    .room(room)
                    .user(user)
                    .role(role)
                    .build();
            memberRepository.save(member);
        }
    }

    private ChatRoomDTO toRoomDTO(ChatRoom room, Long currentUserId) {
        List<UserResponse> members = memberRepository.findByRoomId(room.getId()).stream()
                .map(m -> toUserResponse(m.getUser()))
                .collect(Collectors.toList());

        Page<ChatMessage> lastMsgPage = messageRepository.findByRoomIdOrderByCreatedAtDesc(room.getId(), PageRequest.of(0, 1));
        ChatMessageDTO lastMessage = lastMsgPage.hasContent() ? toMessageDTO(lastMsgPage.getContent().get(0)) : null;

        return ChatRoomDTO.builder()
                .id(room.getId())
                .name(room.getName())
                .roomType(room.getRoomType())
                .avatarUrl(room.getAvatarUrl())
                .createdById(room.getCreatedBy().getId())
                .trainingPlanId(room.getTrainingPlan() != null ? room.getTrainingPlan().getId() : null)
                .challengeId(room.getChallenge() != null ? room.getChallenge().getId() : null)
                .members(members)
                .lastMessage(lastMessage)
                .createdAt(room.getCreatedAt())
                .build();
    }

    private ChatMessageDTO toMessageDTO(ChatMessage msg) {
        return ChatMessageDTO.builder()
                .id(msg.getId())
                .roomId(msg.getRoom().getId())
                .senderId(msg.getSender().getId())
                .senderDisplayName(msg.getSender().getDisplayName())
                .senderAvatarUrl(msg.getSender().getAvatarUrl())
                .content(msg.getContent())
                .messageType(msg.getMessageType())
                .mediaUrl(msg.getMediaUrl())
                .activityId(msg.getActivity() != null ? msg.getActivity().getId() : null)
                .createdAt(msg.getCreatedAt())
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
