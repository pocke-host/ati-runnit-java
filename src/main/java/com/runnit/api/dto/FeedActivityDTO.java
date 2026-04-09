package com.runnit.api.dto;

import com.runnit.api.model.Activity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class FeedActivityDTO {

    // Nested user info — matches the shape frontend expects as `item.user.{id,displayName,avatarUrl}`
    public static class UserInfo {
        private Long id;
        private String displayName;
        private String avatarUrl;

        public UserInfo(Long id, String displayName, String avatarUrl) {
            this.id = id;
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
        }

        public Long getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getAvatarUrl() { return avatarUrl; }
    }

    private Long id;
    private UserInfo user;
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
    private long commentCount;
    // Per-reaction-type counts: { "LIKE": 3, "FIRE": 1, "CLAP": 0 }
    private Map<String, Long> reactionCounts = new HashMap<>();
    // The current authenticated user's reaction type, or null if none
    private String userReaction;

    public static FeedActivityDTO from(Activity a) {
        FeedActivityDTO dto = new FeedActivityDTO();
        dto.id = a.getId();
        dto.user = new UserInfo(
            a.getUser().getId(),
            a.getUser().getDisplayName(),
            a.getUser().getAvatarUrl()
        );
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
    public UserInfo getUser() { return user; }
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
    public long getCommentCount() { return commentCount; }
    public Map<String, Long> getReactionCounts() { return reactionCounts; }
    public String getUserReaction() { return userReaction; }

    public void setCommentCount(long commentCount) { this.commentCount = commentCount; }
    public void setReactionCounts(Map<String, Long> reactionCounts) { this.reactionCounts = reactionCounts; }
    public void setUserReaction(String userReaction) { this.userReaction = userReaction; }
}
