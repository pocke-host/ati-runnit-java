package com.runnit.api.dto;

import com.runnit.api.model.Story;
import java.time.LocalDateTime;

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

    public StoryDTO() {}

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserDisplayName() { return userDisplayName; }
    public String getMediaUrl() { return mediaUrl; }
    public Story.MediaType getMediaType() { return mediaType; }
    public String getCaption() { return caption; }
    public Story.StoryVisibility getVisibility() { return visibility; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public Long getViewCount() { return viewCount; }
    public Boolean getHasViewed() { return hasViewed; }
    public Boolean getIsOwner() { return isOwner; }

    public void setId(Long id) { this.id = id; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }
    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setMediaType(Story.MediaType mediaType) { this.mediaType = mediaType; }
    public void setCaption(String caption) { this.caption = caption; }
    public void setVisibility(Story.StoryVisibility visibility) { this.visibility = visibility; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }
    public void setHasViewed(boolean hasViewed) { this.hasViewed = hasViewed; }
    public void setIsOwner(boolean isOwner) { this.isOwner = isOwner; }
}
