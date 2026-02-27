package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MentorshipMatchDTO {
    private Long id;
    private UserResponse mentor;
    private UserResponse mentee;
    private String sportType;
    private String status;
    private String mentorNotes;
    private String menteeGoals;
    private Instant createdAt;
    private Instant updatedAt;
}
