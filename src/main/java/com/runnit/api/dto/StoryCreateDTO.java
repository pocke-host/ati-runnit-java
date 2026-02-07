// ========== StoryCreateDTO.java ==========
package com.runnit.api.dto;

import com.runnit.api.model.Story;
import lombok.Data;

import java.util.List;

@Data
public class StoryCreateDTO {
    private String mediaUrl;
    private Story.MediaType mediaType;
    private String caption;
    private Story.StoryVisibility visibility = Story.StoryVisibility.PUBLIC;
    private List<Long> closeFriendIds;
}