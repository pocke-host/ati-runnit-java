package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "club_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(ClubMember.ClubMemberId.class)
public class ClubMember {

    @Id
    @Column(name = "club_id")
    private Long clubId;

    @Id
    @Column(name = "user_id")
    private Long userId;

    @CreationTimestamp
    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClubMemberId implements Serializable {
        private Long clubId;
        private Long userId;
    }
}
