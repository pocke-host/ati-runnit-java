package com.runnit.api.dto;

import com.runnit.api.model.Reaction;
import java.time.Instant;
import java.util.Map;

public class MomentResponse {
    private Long id;
    private UserInfo user;
    private Long activityId;
    private String photoUrl;
    private String routeSnapshotUrl;
    private String songTitle;
    private String songArtist;
    private String songLink;
    private Instant createdAt;
    private Long reactionCount;
    private Map<Reaction.ReactionType, Long> reactionsByType;
    private Reaction.ReactionType currentUserReaction;
    private Long commentCount;

    public MomentResponse() {}

    public Long getId() { return id; }
    public UserInfo getUser() { return user; }
    public Long getActivityId() { return activityId; }
    public String getPhotoUrl() { return photoUrl; }
    public String getRouteSnapshotUrl() { return routeSnapshotUrl; }
    public String getSongTitle() { return songTitle; }
    public String getSongArtist() { return songArtist; }
    public String getSongLink() { return songLink; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getReactionCount() { return reactionCount; }
    public Map<Reaction.ReactionType, Long> getReactionsByType() { return reactionsByType; }
    public Reaction.ReactionType getCurrentUserReaction() { return currentUserReaction; }
    public Long getCommentCount() { return commentCount; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Long id;
        private UserInfo user;
        private Long activityId;
        private String photoUrl;
        private String routeSnapshotUrl;
        private String songTitle;
        private String songArtist;
        private String songLink;
        private Instant createdAt;
        private Long reactionCount;
        private Map<Reaction.ReactionType, Long> reactionsByType;
        private Reaction.ReactionType currentUserReaction;
        private Long commentCount;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(UserInfo user) { this.user = user; return this; }
        public Builder activityId(Long activityId) { this.activityId = activityId; return this; }
        public Builder photoUrl(String photoUrl) { this.photoUrl = photoUrl; return this; }
        public Builder routeSnapshotUrl(String routeSnapshotUrl) { this.routeSnapshotUrl = routeSnapshotUrl; return this; }
        public Builder songTitle(String songTitle) { this.songTitle = songTitle; return this; }
        public Builder songArtist(String songArtist) { this.songArtist = songArtist; return this; }
        public Builder songLink(String songLink) { this.songLink = songLink; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder reactionCount(Long reactionCount) { this.reactionCount = reactionCount; return this; }
        public Builder reactionsByType(Map<Reaction.ReactionType, Long> reactionsByType) { this.reactionsByType = reactionsByType; return this; }
        public Builder currentUserReaction(Reaction.ReactionType currentUserReaction) { this.currentUserReaction = currentUserReaction; return this; }
        public Builder commentCount(Long commentCount) { this.commentCount = commentCount; return this; }

        public MomentResponse build() {
            MomentResponse r = new MomentResponse();
            r.id = this.id;
            r.user = this.user;
            r.activityId = this.activityId;
            r.photoUrl = this.photoUrl;
            r.routeSnapshotUrl = this.routeSnapshotUrl;
            r.songTitle = this.songTitle;
            r.songArtist = this.songArtist;
            r.songLink = this.songLink;
            r.createdAt = this.createdAt;
            r.reactionCount = this.reactionCount;
            r.reactionsByType = this.reactionsByType;
            r.currentUserReaction = this.currentUserReaction;
            r.commentCount = this.commentCount;
            return r;
        }
    }

    public static class UserInfo {
        private Long id;
        private String displayName;
        private String avatarUrl;

        public UserInfo() {}

        public Long getId() { return id; }
        public String getDisplayName() { return displayName; }
        public String getAvatarUrl() { return avatarUrl; }

        public static UserInfoBuilder builder() { return new UserInfoBuilder(); }

        public static class UserInfoBuilder {
            private Long id;
            private String displayName;
            private String avatarUrl;

            public UserInfoBuilder id(Long id) { this.id = id; return this; }
            public UserInfoBuilder displayName(String displayName) { this.displayName = displayName; return this; }
            public UserInfoBuilder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }

            public UserInfo build() {
                UserInfo u = new UserInfo();
                u.id = this.id;
                u.displayName = this.displayName;
                u.avatarUrl = this.avatarUrl;
                return u;
            }
        }
    }
}
