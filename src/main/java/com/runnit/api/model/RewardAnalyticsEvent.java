package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "reward_analytics_events")
public class RewardAnalyticsEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(name = "reward_id") private Long rewardId;
    @Column(name = "redemption_id") private Long redemptionId;
    @Column(name = "event_type", nullable = false) private String eventType;
    @Column(name = "metadata_json", columnDefinition = "TEXT") private String metadataJson;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    public static RewardAnalyticsEvent of(Long userId, Long rewardId, Long redemptionId, String type, String metadata){ RewardAnalyticsEvent x=new RewardAnalyticsEvent(); x.userId=userId; x.rewardId=rewardId; x.redemptionId=redemptionId; x.eventType=type; x.metadataJson=metadata; return x; }
}
