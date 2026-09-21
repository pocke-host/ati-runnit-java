package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "reward_admin_actions")
public class RewardAdminAction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "redemption_id", nullable = false) private Long redemptionId;
    @Column(name = "admin_user_id", nullable = false) private Long adminUserId;
    @Column(nullable = false) private String action;
    private String note;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    public static RewardAdminAction of(Long redemptionId, Long adminUserId, String action, String note){ RewardAdminAction x=new RewardAdminAction(); x.redemptionId=redemptionId; x.adminUserId=adminUserId; x.action=action; x.note=note; return x; }
}
