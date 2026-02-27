package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ChatRoomDTO {
    private Long id;
    private String name;
    private String roomType;
    private String avatarUrl;
    private Long createdById;
    private Long trainingPlanId;
    private Long challengeId;
    private List<UserResponse> members;
    private ChatMessageDTO lastMessage;
    private Instant createdAt;
}
