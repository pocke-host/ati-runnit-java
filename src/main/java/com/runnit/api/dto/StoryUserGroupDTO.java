package com.runnit.api.dto;

import lombok.Data;
import java.util.List;
// import com.runnit.api.dto.StoryDTO;

@Data
public class StoryUserGroupDTO {
    private Long userId;
    private String userDisplayName;
    private String userAvatar;
    private Boolean hasUnviewed;
    private List<StoryDTO> stories;
}