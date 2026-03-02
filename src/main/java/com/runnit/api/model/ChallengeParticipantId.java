package com.runnit.api.model;

import java.io.Serializable;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChallengeParticipantId implements Serializable {
    private Long challengeId;
    private Long userId;
}
