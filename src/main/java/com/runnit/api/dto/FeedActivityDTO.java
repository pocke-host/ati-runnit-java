package com.runnit.api.dto;

import com.runnit.api.model.Activity;

import java.time.LocalDateTime;

public class FeedActivityDTO {

    private Long id;
    private Long userId;
    private String userDisplayName;
    private String userAvatarUrl;
    private String sportType;
    private Integer durationSeconds;
    private Integer distanceMeters;
    private Integer elevationGain;
    private Integer calories;
    private Double averagePace;
    private Integer averageHeartRate;
    private String routePolyline;
    private Double startLat;
    private Double startLng;
    private String notes;
    private String source;
    private LocalDateTime createdAt;
    private long reactionCount;
    private long commentCount;
    private String userReactionType;

    public static FeedActivityDTO from(Activity a) {
        FeedActivityDTO dto = new FeedActivityDTO();
        dto.id = a.getId();
        dto.userId = a.getUser().getId();
        dto.userDisplayName = a.getUser().getDisplayName();
        dto.userAvatarUrl = a.getUser().getAvatarUrl();
        dto.sportType = a.getSportType() != null ? a.getSportType().name() : null;
        dto.durationSeconds = a.getDurationSeconds();
        dto.distanceMeters = a.getDistanceMeters();
        dto.elevationGain = a.getElevationGain();
        dto.calories = a.getCalories();
        dto.averagePace = a.getAveragePace();
        dto.averageHeartRate = a.getAverageHeartRate();
        dto.routePolyline = a.getRoutePolyline();
        dto.startLat = a.getStartLat();
        dto.startLng = a.getStartLng();
        dto.notes = a.getNotes();
        dto.source = a.getSource() != null ? a.getSource().name() : null;
        dto.createdAt = a.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getUserDisplayName() { return userDisplayName; }
    public String getUserAvatarUrl() { return userAvatarUrl; }
    public String getSportType() { return sportType; }
    public Integer getDurationSeconds() { return durationSeconds; }
    public Integer getDistanceMeters() { return distanceMeters; }
    public Integer getElevationGain() { return elevationGain; }
    public Integer getCalories() { return calories; }
    public Double getAveragePace() { return averagePace; }
    public Integer getAverageHeartRate() { return averageHeartRate; }
    public String getRoutePolyline() { return routePolyline; }
    public Double getStartLat() { return startLat; }
    public Double getStartLng() { return startLng; }
    public String getNotes() { return notes; }
    public String getSource() { return source; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public long getReactionCount() { return reactionCount; }
    public long getCommentCount() { return commentCount; }
    public String getUserReactionType() { return userReactionType; }

    public void setReactionCount(long reactionCount) { this.reactionCount = reactionCount; }
    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public void setUserReactionType(String userReactionType) { this.userReactionType = userReactionType; }
}
