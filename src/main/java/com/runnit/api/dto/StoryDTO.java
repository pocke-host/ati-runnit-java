// ========== StoryDTO.java ==========
package com.runnit.api.dto;

import com.runnit.api.model.Story;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StoryDTO {
    private Long id;
    private Long userId;
    private String userDisplayName;
    private String mediaUrl;
    private Story.MediaType mediaType;
    private String caption;
    private Story.StoryVisibility visibility;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long viewCount;
    private Boolean hasViewed;
    private Boolean isOwner;
}
