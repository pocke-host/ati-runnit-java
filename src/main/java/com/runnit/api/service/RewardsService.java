package com.runnit.api.service;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RewardsService {
    private final UserRepository users;
    private final ActivityRepository activities;
    private final RewardCatalogRepository catalog;
    private final RewardLedgerRepository ledger;
    private final RewardRedemptionRepository redemptions;
    private final RewardAnalyticsEventRepository analytics;
    private final RewardAbuseFlagRepository abuseFlags;
    private final NotificationRepository notifications;
    private final EmailService emailService;
    @Value("${stripe.secret.key:}") private String stripeSecretKey;
    @Value("${app.frontend.url:http://localhost:5173}") private String frontendUrl;

    @Transactional
    public void awardForActivity(Activity activity) {
        if (activity == null || activity.getUser() == null || activity.getId() == null) return;
        String source = activity.getSource() == null ? "MANUAL" : activity.getSource().name();
        String external = activity.getExternalId();
        String key = external == null || external.isBlank() ? "activity:" + activity.getId() : source + ":" + external;
        int points = "MANUAL".equals(source) ? 10 : 25;
        int durationBonus = activity.getDurationSeconds() == null ? 0 : Math.min(50, Math.max(0, activity.getDurationSeconds()) / 600);
        award(activity.getUser(), points + durationBonus, "ACTIVITY", key, "Workout logged");
    }

    @Transactional
    public Map<String,Object> dashboard(Long userId) {
        users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        activities.findAllByUserId(userId).forEach(this::awardForActivity);
        analytics.save(RewardAnalyticsEvent.of(userId, null, null, "CATALOG_VIEW", null));
        Map<String,Object> response=new LinkedHashMap<>(); response.put("balance",ledger.balance(userId));
        response.put("catalog",catalog.findByActiveTrueOrderByPointsCostAsc().stream().map(this::catalogMap).toList());
        response.put("redemptions",redemptions.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::redemptionMap).toList());
        response.put("recentActivity",ledger.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream().map(this::ledgerMap).toList()); return response;
    }

    @Transactional
    public Map<String,Object> redeem(Long userId, Long rewardId, Map<String,String> body) {
        User user=users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found")); ensureEligible(userId);
        RewardCatalogItem reward=findActive(rewardId); if("PAID".equalsIgnoreCase(reward.getRewardType())) throw new BadRequestException("Use the partner checkout for this item");
        if(reward.getInventory()!=null && reward.getInventory()<=0) throw new BadRequestException("This reward is out of stock");
        long balance=ledger.balance(userId); if(balance<reward.getPointsCost()) throw new BadRequestException("You need " + (reward.getPointsCost()-balance) + " more points");
        String name=value(body,"shippingName"), address=value(body,"shippingAddress"), size=value(body,"apparelSize");
        if(!"DIGITAL".equalsIgnoreCase(reward.getCategory()) && (name==null||name.isBlank()||address==null||address.isBlank())) throw new BadRequestException("Shipping name and address are required for this reward");
        if("APPAREL".equalsIgnoreCase(reward.getCategory()) && (size==null||size.isBlank())) throw new BadRequestException("Select an apparel size");
        if(size!=null && reward.getSizesJson()!=null && !reward.getSizesJson().contains("\""+size+"\"")) throw new BadRequestException("That apparel size is unavailable");
        award(user,-reward.getPointsCost(),"REDEMPTION","redemption:"+UUID.randomUUID(),"Redeemed "+reward.getTitle());
        RewardRedemption redemption=redemptions.save(RewardRedemption.of(user,reward,name,address)); redemption.setApparelSize(size); redemption.setShippingMethod(value(body,"shippingMethod")); redemptions.save(redemption);
        decrementInventory(reward); analytics.save(RewardAnalyticsEvent.of(userId,rewardId,redemption.getId(),"REDEMPTION_CREATED",null)); notifyAndEmail(user,"REWARD_REQUESTED","Your reward redemption is being prepared.",redemption); return redemptionMap(redemption);
    }

    @Transactional
    public Map<String,Object> createPaidCheckout(Long userId, Long rewardId, Map<String,String> body) {
        User user=users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found")); RewardCatalogItem reward=findActive(rewardId);
        if(!"PAID".equalsIgnoreCase(reward.getRewardType())||reward.getPriceCents()==null) throw new BadRequestException("This reward does not use paid checkout");
        if(stripeSecretKey.isBlank()) throw new BadRequestException("Paid rewards are not configured yet"); if(reward.getInventory()!=null&&reward.getInventory()<=0) throw new BadRequestException("This reward is out of stock");
        Stripe.apiKey=stripeSecretKey;
        try {
            RewardRedemption r=redemptions.save(RewardRedemption.of(user,reward,value(body,"shippingName"),value(body,"shippingAddress"))); r.setStatus("PAYMENT_PENDING"); r.setSubtotalCents(reward.getPriceCents()); r.setTotalCents(reward.getPriceCents()); redemptions.save(r);
            SessionCreateParams params=SessionCreateParams.builder().setMode(SessionCreateParams.Mode.PAYMENT).setCustomerEmail(user.getEmail()).setSuccessUrl(frontendUrl+"/rewards?paid=1").setCancelUrl(frontendUrl+"/rewards?cancelled=1")
                .addLineItem(SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData(SessionCreateParams.LineItem.PriceData.builder().setCurrency("usd").setUnitAmount((long)reward.getPriceCents()).setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder().setName(reward.getTitle()).build()).build()).build())
                .putMetadata("reward_redemption_id",String.valueOf(r.getId())).putMetadata("user_id",String.valueOf(userId)).build();
            Session session=Session.create(params); r.setExternalCheckoutSessionId(session.getId()); redemptions.save(r); analytics.save(RewardAnalyticsEvent.of(userId,rewardId,r.getId(),"PAID_CHECKOUT_STARTED",null)); return Map.of("url",session.getUrl(),"sessionId",session.getId());
        } catch(Exception e) { throw new BadRequestException("Unable to start partner checkout"); }
    }

    @Transactional public void completePaidCheckout(String sessionId) { redemptions.findByExternalCheckoutSessionId(sessionId).ifPresent(r -> { if(!"PAYMENT_PENDING".equals(r.getStatus())) return; r.setStatus("REQUESTED"); redemptions.save(r); decrementInventory(r.getReward()); notifyAndEmail(r.getUser(),"REWARD_REQUESTED","Your paid reward order has been received.",r); }); }

    @Transactional public Map<String,Object> cancel(Long userId, Long redemptionId, String reason) {
        RewardRedemption r=redemptions.findById(redemptionId).orElseThrow(() -> new ResourceNotFoundException("Redemption not found")); if(!Objects.equals(r.getUser().getId(),userId)) throw new BadRequestException("You cannot cancel this redemption");
        if(!Set.of("REQUESTED","PAYMENT_PENDING").contains(r.getStatus())) throw new BadRequestException("This redemption can no longer be cancelled"); r.setStatus("CANCELLED"); r.setCancellationReason(reason); redemptions.save(r);
        if(r.getPointsCost()!=null&&r.getPointsCost()>0) award(r.getUser(),r.getPointsCost(),"REFUND","refund:"+redemptionId,"Refund for cancelled reward"); analytics.save(RewardAnalyticsEvent.of(userId,r.getReward().getId(),redemptionId,"REDEMPTION_CANCELLED",null)); return redemptionMap(r);
    }

    private RewardCatalogItem findActive(Long id){return catalog.findById(id).filter(r->Boolean.TRUE.equals(r.getActive())).orElseThrow(()->new ResourceNotFoundException("Reward not found"));}
    private void ensureEligible(Long id){if(abuseFlags.existsByUserIdAndStatus(id,"OPEN")) throw new BadRequestException("Rewards are temporarily unavailable while your account is reviewed");}
    private void decrementInventory(RewardCatalogItem r){if(r.getInventory()!=null){r.setInventory(r.getInventory()-1);catalog.save(r);}}
    private String value(Map<String,String> b,String k){return b==null?null:b.get(k);}
    private void award(User u,int points,String type,String key,String description){if(!ledger.existsByUserIdAndEventKey(u.getId(),key)) ledger.save(RewardLedgerEntry.of(u,points,type,key,description));}
    private void notifyAndEmail(User u,String type,String message,RewardRedemption r){notifications.save(Notification.builder().user(u).type(type).message(message).referenceId(r.getId()).referenceType("REWARD_REDEMPTION").build()); if(u.getEmail()!=null) emailService.sendRewardUpdate(u.getEmail(),r.getReward().getTitle(),r.getStatus());}
    private Map<String,Object> catalogMap(RewardCatalogItem r){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("title",r.getTitle());m.put("description",r.getDescription());m.put("category",r.getCategory());m.put("rewardType",r.getRewardType());m.put("pointsCost",r.getPointsCost());m.put("priceCents",r.getPriceCents());m.put("purchaseUrl",r.getPurchaseUrl());m.put("imageUrl",r.getImageUrl());m.put("inventory",r.getInventory());m.put("sizesJson",r.getSizesJson());m.put("partnerName",r.getPartnerName());return m;}
    private Map<String,Object> ledgerMap(RewardLedgerEntry e){Map<String,Object> m=new LinkedHashMap<>();m.put("id",e.getId());m.put("pointsDelta",e.getPointsDelta());m.put("eventType",e.getEventType());m.put("description",e.getDescription());m.put("createdAt",e.getCreatedAt());return m;}
    public Map<String,Object> redemptionMap(RewardRedemption r){Map<String,Object> m=new LinkedHashMap<>();m.put("id",r.getId());m.put("rewardId",r.getReward().getId());m.put("title",r.getReward().getTitle());m.put("pointsCost",r.getPointsCost());m.put("priceCents",r.getPriceCents());m.put("status",r.getStatus());m.put("apparelSize",r.getApparelSize());m.put("shippingName",r.getShippingName());m.put("shippingAddress",r.getShippingAddress());m.put("createdAt",r.getCreatedAt());m.put("shippedAt",r.getShippedAt());m.put("fulfilledAt",r.getFulfilledAt());return m;}
}
