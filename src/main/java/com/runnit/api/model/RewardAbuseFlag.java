package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "reward_abuse_flags")
public class RewardAbuseFlag {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "flag_type", nullable = false) private String flagType;
    @Column(nullable = false) private String reason;
    @Column(nullable = false) private String status = "OPEN";
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    public Long getId(){return id;}
    public static RewardAbuseFlag of(Long userId, String type, String reason){ RewardAbuseFlag x=new RewardAbuseFlag(); x.userId=userId; x.flagType=type; x.reason=reason; return x; }
}
