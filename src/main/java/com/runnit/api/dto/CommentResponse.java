package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private Long userId;
    private String userDisplayName;
    private String userAvatarUrl;
    private Long momentId;
    private String content;
    private Instant createdAt;
}
