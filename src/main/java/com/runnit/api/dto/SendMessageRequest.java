package com.runnit.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotBlank
    private String content;
    private String messageType = "TEXT"; // TEXT, IMAGE, ACTIVITY_SHARE
    private String mediaUrl;
    private Long activityId;
}
