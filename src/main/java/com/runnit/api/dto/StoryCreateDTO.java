package com.runnit.api.dto;

import com.runnit.api.model.Story;
import java.util.List;

public class StoryCreateDTO {
    private String mediaUrl;
    private Story.MediaType mediaType;
    private String caption;
    private Story.StoryVisibility visibility = Story.StoryVisibility.PUBLIC;
    private List<Long> closeFriendIds;

    public StoryCreateDTO() {}

    public String getMediaUrl() { return mediaUrl; }
    public Story.MediaType getMediaType() { return mediaType; }
    public String getCaption() { return caption; }
    public Story.StoryVisibility getVisibility() { return visibility; }
    public List<Long> getCloseFriendIds() { return closeFriendIds; }

    public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }
    public void setMediaType(Story.MediaType mediaType) { this.mediaType = mediaType; }
    public void setCaption(String caption) { this.caption = caption; }
    public void setVisibility(Story.StoryVisibility visibility) { this.visibility = visibility; }
    public void setCloseFriendIds(List<Long> closeFriendIds) { this.closeFriendIds = closeFriendIds; }
}
