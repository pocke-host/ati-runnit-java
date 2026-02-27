package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ChatMessageDTO {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderDisplayName;
    private String senderAvatarUrl;
    private String content;
    private String messageType;
    private String mediaUrl;
    private Long activityId;
    private Instant createdAt;
}
