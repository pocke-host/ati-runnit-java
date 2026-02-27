package com.runnit.api.dto;

import lombok.Data;

@Data
public class MentorshipRequest {
    private Long mentorId;
    private String sportType;
    private String menteeGoals;
}
