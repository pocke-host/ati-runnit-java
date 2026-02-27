package com.runnit.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class ChallengeResponse {
    private Long id;
    private Long creatorId;
    private String creatorDisplayName;
    private String title;
    private String description;
    private String sportType;
    private String challengeType;
    private double goalValue;
    private String goalUnit;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isGroup;
    private Integer maxParticipants;
    private boolean isPublic;
    private String charityName;
    private String charityUrl;
    private String coverImageUrl;
    private long participantCount;
    private boolean isJoined;
    private Double currentUserProgress;
    private boolean currentUserCompleted;
    private Instant createdAt;
}
