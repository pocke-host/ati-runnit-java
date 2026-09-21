package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "rewards_catalog")
public class RewardCatalogItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(nullable = false) private String category;
    @Column(name = "reward_type", nullable = false) private String rewardType;
    @Column(name = "points_cost", nullable = false) private Integer pointsCost = 0;
    @Column(name = "price_cents") private Integer priceCents;
    @Column(name = "purchase_url") private String purchaseUrl;
    @Column(name = "image_url") private String imageUrl;
    private Integer inventory;
    @Column(nullable = false) private Boolean active = true;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public Long getId(){return id;} public String getTitle(){return title;} public String getDescription(){return description;}
    public String getCategory(){return category;} public String getRewardType(){return rewardType;} public Integer getPointsCost(){return pointsCost;}
    public Integer getPriceCents(){return priceCents;} public String getPurchaseUrl(){return purchaseUrl;} public String getImageUrl(){return imageUrl;}
    public Integer getInventory(){return inventory;} public Boolean getActive(){return active;} public Instant getCreatedAt(){return createdAt;}
    public void setInventory(Integer inventory){this.inventory=inventory;}
}
