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
    @Column(name = "apparel_size") private String apparelSize;
    @Column(name = "subtotal_cents") private Integer subtotalCents;
    @Column(name = "shipping_cents") private Integer shippingCents;
    @Column(name = "tax_cents") private Integer taxCents;
    @Column(name = "total_cents") private Integer totalCents;
    @Column(name = "shipping_method") private String shippingMethod;
    @Column(name = "external_checkout_session_id") private String externalCheckoutSessionId;
    @Column(name = "cancellation_reason") private String cancellationReason;
    @Column(name = "shipped_at") private Instant shippedAt;
    @Column(name = "fulfilled_at") private Instant fulfilledAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    /**
     * The database column is NOT NULL, and Hibernate includes null fields in
     * the insert statement. Set both timestamps in the entity lifecycle so a
     * redemption cannot fail before MySQL gets a chance to apply its default.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId(){return id;} public User getUser(){return user;} public RewardCatalogItem getReward(){return reward;}
    public Integer getPointsCost(){return pointsCost;} public Integer getPriceCents(){return priceCents;} public String getStatus(){return status;}
    public String getShippingName(){return shippingName;} public String getShippingAddress(){return shippingAddress;} public Instant getCreatedAt(){return createdAt;}
    public String getApparelSize(){return apparelSize;} public Integer getSubtotalCents(){return subtotalCents;} public Integer getShippingCents(){return shippingCents;} public Integer getTaxCents(){return taxCents;} public Integer getTotalCents(){return totalCents;} public String getShippingMethod(){return shippingMethod;} public String getExternalCheckoutSessionId(){return externalCheckoutSessionId;} public String getCancellationReason(){return cancellationReason;} public Instant getShippedAt(){return shippedAt;} public Instant getFulfilledAt(){return fulfilledAt;}
    public void setStatus(String v){status=v;} public void setApparelSize(String v){apparelSize=v;} public void setCancellationReason(String v){cancellationReason=v;} public void setShippedAt(Instant v){shippedAt=v;} public void setFulfilledAt(Instant v){fulfilledAt=v;} public void setExternalCheckoutSessionId(String v){externalCheckoutSessionId=v;} public void setSubtotalCents(Integer v){subtotalCents=v;} public void setShippingCents(Integer v){shippingCents=v;} public void setTaxCents(Integer v){taxCents=v;} public void setTotalCents(Integer v){totalCents=v;} public void setShippingMethod(String v){shippingMethod=v;}
    public static RewardRedemption of(User u, RewardCatalogItem r, String name, String address){
        RewardRedemption x=new RewardRedemption(); x.user=u; x.reward=r; x.pointsCost=r.getPointsCost(); x.priceCents=r.getPriceCents(); x.shippingName=name; x.shippingAddress=address; return x;
    }
}
