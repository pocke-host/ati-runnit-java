// ========== MomentResponse.java ==========
package com.runnit.api.dto;

import com.runnit.api.model.Reaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentResponse {
    private Long id;
    private Long userId;
    private String userDisplayName;
    private String userAvatarUrl;
    private Long activityId;
    private String photoUrl;
    private String routeSnapshotUrl;
    private String songTitle;
    private String songArtist;
    private String songLink;
    private Instant createdAt;
    private Long reactionCount;
    private Map<Reaction.ReactionType, Long> reactionsByType;
    private Reaction.ReactionType currentUserReaction; // null if no reaction
}




