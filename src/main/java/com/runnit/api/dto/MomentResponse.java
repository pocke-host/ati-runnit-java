package com.runnit.api.dto;

import com.runnit.api.model.Reaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String displayName;
        private String avatarUrl;
    }
}