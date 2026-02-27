package com.runnit.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateRoomRequest {
    private String name;
    @NotBlank
    private String roomType; // DIRECT, GROUP, COMMUNITY
    private String avatarUrl;
    private List<Long> memberIds;
    private Long trainingPlanId;
    private Long challengeId;
}
