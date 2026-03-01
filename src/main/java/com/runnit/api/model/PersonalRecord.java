package com.runnit.api.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "personal_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonalRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "best_5k")
    private Integer best5k;

    @Column(name = "best_10k")
    private Integer best10k;

    @Column(name = "best_half")
    private Integer bestHalf;

    @Column(name = "best_marathon")
    private Integer bestMarathon;

    @Column(name = "longest_run")
    private Integer longestRun;

    @Column(name = "longest_ride")
    private Integer longestRide;

    @Column(name = "most_elevation")
    private Integer mostElevation;

    @Column(name = "fastest_pace")
    private Double fastestPace;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
