package com.runnit.api.dto;

import java.util.List;

public class StoryUserGroupDTO {
    private Long userId;
    private String userDisplayName;
    private String userAvatar;
    private Boolean hasUnviewed;
    private List<StoryDTO> stories;

    public StoryUserGroupDTO() {}

    public Long getUserId() { return userId; }
    public String getUserDisplayName() { return userDisplayName; }
    public String getUserAvatar() { return userAvatar; }
    public Boolean getHasUnviewed() { return hasUnviewed; }
    public List<StoryDTO> getStories() { return stories; }

    public void setUserId(Long userId) { this.userId = userId; }
    public void setUserDisplayName(String userDisplayName) { this.userDisplayName = userDisplayName; }
    public void setUserAvatar(String userAvatar) { this.userAvatar = userAvatar; }
    public void setHasUnviewed(boolean hasUnviewed) { this.hasUnviewed = hasUnviewed; }
    public void setStories(List<StoryDTO> stories) { this.stories = stories; }
}
