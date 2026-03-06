package com.runnit.api.dto;

import jakarta.validation.constraints.*;

public class MomentRequest {

    private Long activityId;

    @NotBlank(message = "Photo URL is required")
    private String photoUrl;

    private String routeSnapshotUrl;

    @NotBlank(message = "Song title is required")
    @Size(max = 255, message = "Song title too long")
    private String songTitle;

    @NotBlank(message = "Song artist is required")
    @Size(max = 255, message = "Song artist too long")
    private String songArtist;

    @Size(max = 500, message = "Song link too long")
    private String songLink;

    public MomentRequest() {}

    public Long getActivityId() { return activityId; }
    public String getPhotoUrl() { return photoUrl; }
    public String getRouteSnapshotUrl() { return routeSnapshotUrl; }
    public String getSongTitle() { return songTitle; }
    public String getSongArtist() { return songArtist; }
    public String getSongLink() { return songLink; }

    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
    public void setRouteSnapshotUrl(String routeSnapshotUrl) { this.routeSnapshotUrl = routeSnapshotUrl; }
    public void setSongTitle(String songTitle) { this.songTitle = songTitle; }
    public void setSongArtist(String songArtist) { this.songArtist = songArtist; }
    public void setSongLink(String songLink) { this.songLink = songLink; }
}
