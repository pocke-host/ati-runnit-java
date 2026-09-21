package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "reward_redemptions")
public class RewardRedemption {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private User user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reward_id", nullable = false) private RewardCatalogItem reward;
    @Column(name = "points_cost", nullable = false) private Integer pointsCost;
    @Column(name = "price_cents") private Integer priceCents;
    @Column(nullable = false) private String status = "REQUESTED";
    @Column(name = "shipping_name") private String shippingName;
    @Column(name = "shipping_address", columnDefinition = "TEXT") private String shippingAddress;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public Long getId(){return id;} public User getUser(){return user;} public RewardCatalogItem getReward(){return reward;}
    public Integer getPointsCost(){return pointsCost;} public Integer getPriceCents(){return priceCents;} public String getStatus(){return status;}
    public String getShippingName(){return shippingName;} public String getShippingAddress(){return shippingAddress;} public Instant getCreatedAt(){return createdAt;}
    public static RewardRedemption of(User u, RewardCatalogItem r, String name, String address){
        RewardRedemption x=new RewardRedemption(); x.user=u; x.reward=r; x.pointsCost=r.getPointsCost(); x.priceCents=r.getPriceCents(); x.shippingName=name; x.shippingAddress=address; return x;
    }
}
