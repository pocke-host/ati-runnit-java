package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeaderboardEntryDTO {
    private int rank;
    private Long userId;
    private String displayName;
    private String avatarUrl;
    private double currentValue;
    private boolean isCompleted;
}
