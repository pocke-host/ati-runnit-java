package com.runnit.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Locale;

@Entity
@Table(name = "reward_coupon_codes")
public class RewardCouponCode {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "reward_id", nullable = false) private Long rewardId;
    @Column(nullable = false, unique = true) private String code;
    @Column(name = "discount_type", nullable = false) private String discountType = "PERCENT";
    @Column(name = "discount_value", nullable = false) private Integer discountValue;
    @Column(name = "max_redemptions") private Integer maxRedemptions;
    @Column(name = "redeemed_count", nullable = false) private Integer redeemedCount = 0;
    @Column(name = "expires_at") private Instant expiresAt;
    @Column(nullable = false) private Boolean active = true;
    public Long getId(){return id;} public Long getRewardId(){return rewardId;} public String getCode(){return code;} public String getDiscountType(){return discountType;} public Integer getDiscountValue(){return discountValue;} public Integer getMaxRedemptions(){return maxRedemptions;} public Integer getRedeemedCount(){return redeemedCount;} public Instant getExpiresAt(){return expiresAt;} public Boolean getActive(){return active;}
    public static RewardCouponCode of(Long rewardId, String code, String type, Integer value, Integer max, Instant expiresAt){ RewardCouponCode x=new RewardCouponCode(); x.rewardId=rewardId; x.code=code.toUpperCase(Locale.ROOT); x.discountType=type==null?"PERCENT":type; x.discountValue=value; x.maxRedemptions=max; x.expiresAt=expiresAt; return x; }
    public void setActive(Boolean v){active=v;} public void setRedeemedCount(Integer v){redeemedCount=v;}
}
