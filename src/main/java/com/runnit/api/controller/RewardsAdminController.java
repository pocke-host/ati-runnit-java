package com.runnit.api.controller;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import com.runnit.api.service.EmailService;
import com.runnit.api.service.RewardsService;
import com.stripe.Stripe;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin/rewards")
@RequiredArgsConstructor
public class RewardsAdminController {
    private final UserRepository users; private final RewardRedemptionRepository redemptions; private final RewardCatalogRepository catalog;
    private final RewardAdminActionRepository actions; private final RewardAnalyticsEventRepository analytics; private final RewardAbuseFlagRepository abuseFlags;
    private final RewardCouponCodeRepository coupons; private final RewardLedgerRepository ledger; private final RewardsService rewards; private final NotificationRepository notifications; private final EmailService emailService;
    @Value("${stripe.secret.key:}") private String stripeSecretKey;

    @GetMapping
    public ResponseEntity<?> dashboard(Authentication auth) {
        User admin=admin(auth); Map<String,Object> out=new LinkedHashMap<>(); out.put("redemptions",redemptions.findTop100ByOrderByCreatedAtDesc().stream().map(rewards::redemptionMap).toList());
        Map<String,Long> metrics=new LinkedHashMap<>(); for(String type:List.of("CATALOG_VIEW","REDEMPTION_CREATED","PAID_CHECKOUT_STARTED","REDEMPTION_CANCELLED")) metrics.put(type,analytics.countByEventType(type)); out.put("analytics",metrics); out.put("adminId",admin.getId()); return ResponseEntity.ok(out);
    }

    @PatchMapping("/redemptions/{id}/status") @Transactional
    public ResponseEntity<?> updateStatus(@PathVariable Long id,@RequestBody Map<String,String> body,Authentication auth){
        User admin=admin(auth); RewardRedemption r=redemptions.findById(id).orElseThrow(()->new ResourceNotFoundException("Redemption not found")); String status=body.get("status");
        if(!Set.of("SHIPPED","FULFILLED","CANCELLED","REFUNDED").contains(status)) throw new BadRequestException("Unsupported reward status");
        if(Set.of("CANCELLED","REFUNDED").contains(status)&&r.getPointsCost()!=null&&r.getPointsCost()>0&&!ledger.existsByUserIdAndEventKey(r.getUser().getId(),"refund:"+id)){ledger.save(RewardLedgerEntry.of(r.getUser(),r.getPointsCost(),"REFUND","refund:"+id,"Refund for reward "+status.toLowerCase()));}
        if("REFUNDED".equals(status)&&r.getExternalCheckoutSessionId()!=null&&!stripeSecretKey.isBlank()&&!refundPaidCheckout(r)) throw new BadRequestException("Stripe refund could not be completed");
        r.setStatus(status); r.setCancellationReason(body.get("note")); if("SHIPPED".equals(status)) r.setShippedAt(Instant.now()); if("FULFILLED".equals(status)) r.setFulfilledAt(Instant.now()); redemptions.save(r);
        actions.save(RewardAdminAction.of(id,admin.getId(),status,body.get("note"))); notifications.save(Notification.builder().user(r.getUser()).type("REWARD_"+status).message("Your reward is now "+status.toLowerCase()+".").referenceId(id).referenceType("REWARD_REDEMPTION").build()); if(r.getUser().getEmail()!=null) emailService.sendRewardUpdate(r.getUser().getEmail(),r.getReward().getTitle(),status); return ResponseEntity.ok(rewards.redemptionMap(r));
    }

    @GetMapping("/catalog/{rewardId}/coupons") public ResponseEntity<?> coupons(@PathVariable Long rewardId,Authentication auth){admin(auth); return ResponseEntity.ok(coupons.findByRewardIdOrderByIdDesc(rewardId));}
    @PostMapping("/catalog/{rewardId}/coupons") public ResponseEntity<?> createCoupon(@PathVariable Long rewardId,@RequestBody Map<String,Object> body,Authentication auth){admin(auth); catalog.findById(rewardId).orElseThrow(()->new ResourceNotFoundException("Reward not found")); String code=String.valueOf(body.getOrDefault("code","")); if(code.isBlank()) throw new BadRequestException("Coupon code is required"); Integer value=Integer.valueOf(String.valueOf(body.getOrDefault("discountValue","0"))); Integer max=body.get("maxRedemptions")==null?null:Integer.valueOf(String.valueOf(body.get("maxRedemptions"))); RewardCouponCode c=coupons.save(RewardCouponCode.of(rewardId,code,String.valueOf(body.getOrDefault("discountType","PERCENT")),value,max,null)); return ResponseEntity.ok(Map.of("id",c.getId(),"code",c.getCode()));}
    @PostMapping("/abuse-flags") public ResponseEntity<?> flag(@RequestBody Map<String,Object> body,Authentication auth){User admin=admin(auth); Long userId=Long.valueOf(String.valueOf(body.get("userId"))); RewardAbuseFlag f=abuseFlags.save(RewardAbuseFlag.of(userId,String.valueOf(body.getOrDefault("flagType","MANUAL_REVIEW")),String.valueOf(body.getOrDefault("reason","Admin review")))); return ResponseEntity.ok(Map.of("flagged",true,"adminId",admin.getId(),"flagId",f.getId()));}

    private User admin(Authentication auth){User u=users.findById((Long)auth.getPrincipal()).orElseThrow(()->new ResourceNotFoundException("User not found")); if(!"admin".equalsIgnoreCase(u.getRole())) throw new BadRequestException("Admin access required"); return u;}
    private boolean refundPaidCheckout(RewardRedemption r){try{Stripe.apiKey=stripeSecretKey; Session s=Session.retrieve(r.getExternalCheckoutSessionId()); if(s.getPaymentIntent()==null) return false; Refund.create(RefundCreateParams.builder().setPaymentIntent(s.getPaymentIntent()).build()); return true;}catch(Exception ignored){return false;}}
}
