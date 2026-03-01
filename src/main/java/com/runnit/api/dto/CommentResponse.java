package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CommentResponse {
    private Long id;
    private String text;
    private Instant createdAt;
    private UserInfo user;

    @Data
    @Builder
    public static class UserInfo {
        private Long id;
        private String displayName;
        private String avatarUrl;
    }
}
