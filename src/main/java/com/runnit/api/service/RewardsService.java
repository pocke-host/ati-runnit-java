package com.runnit.api.service;

import com.runnit.api.exception.BadRequestException;
import com.runnit.api.exception.ResourceNotFoundException;
import com.runnit.api.model.*;
import com.runnit.api.repository.*;
import lombok.RequiredArgsConstructor;
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

    /** Earn once per activity. Imported workouts receive a modest verification bonus. */
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
    public Map<String, Object> dashboard(Long userId) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        // Backfill activity points lazily so existing athletes receive credit once, without
        // a risky bulk migration or duplicate awards.
        activities.findAllByUserId(userId).forEach(this::awardForActivity);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("balance", ledger.balance(userId));
        response.put("catalog", catalog.findByActiveTrueOrderByPointsCostAsc().stream().map(this::catalogMap).toList());
        response.put("redemptions", redemptions.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::redemptionMap).toList());
        response.put("recentActivity", ledger.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream().map(this::ledgerMap).toList());
        return response;
    }

    @Transactional
    public Map<String, Object> redeem(Long userId, Long rewardId, Map<String, String> body) {
        User user = users.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found"));
        RewardCatalogItem reward = catalog.findById(rewardId).filter(r -> Boolean.TRUE.equals(r.getActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Reward not found"));
        if ("PAID".equalsIgnoreCase(reward.getRewardType())) {
            throw new BadRequestException("This item is available to purchase through its partner link, not with points");
        }
        if (reward.getInventory() != null && reward.getInventory() <= 0) throw new BadRequestException("This reward is out of stock");
        long balance = ledger.balance(userId);
        if (balance < reward.getPointsCost()) throw new BadRequestException("You need " + (reward.getPointsCost() - balance) + " more points");
        String name = body == null ? null : body.get("shippingName");
        String address = body == null ? null : body.get("shippingAddress");
        if (!"DIGITAL".equalsIgnoreCase(reward.getCategory()) && (name == null || name.isBlank() || address == null || address.isBlank())) {
            throw new BadRequestException("Shipping name and address are required for this reward");
        }
        String eventKey = "redemption:" + UUID.randomUUID();
        award(user, -reward.getPointsCost(), "REDEMPTION", eventKey, "Redeemed " + reward.getTitle());
        RewardRedemption redemption = redemptions.save(RewardRedemption.of(user, reward, name, address));
        if (reward.getInventory() != null) reward.setInventory(reward.getInventory() - 1);
        // Catalog inventory is intentionally admin-controlled in this MVP; the entity is
        // updated through the managed transaction without exposing a public stock endpoint.
        catalog.save(reward);
        return redemptionMap(redemption);
    }

    private void award(User user, int points, String type, String key, String description) {
        if (!ledger.existsByUserIdAndEventKey(user.getId(), key)) ledger.save(RewardLedgerEntry.of(user, points, type, key, description));
    }

    private Map<String, Object> catalogMap(RewardCatalogItem r) {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("id", r.getId()); m.put("title", r.getTitle()); m.put("description", r.getDescription());
        m.put("category", r.getCategory()); m.put("rewardType", r.getRewardType()); m.put("pointsCost", r.getPointsCost()); m.put("priceCents", r.getPriceCents());
        m.put("purchaseUrl", r.getPurchaseUrl()); m.put("imageUrl", r.getImageUrl()); m.put("inventory", r.getInventory()); return m;
    }
    private Map<String, Object> ledgerMap(RewardLedgerEntry e) { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",e.getId()); m.put("pointsDelta",e.getPointsDelta()); m.put("eventType",e.getEventType()); m.put("description",e.getDescription()); m.put("createdAt",e.getCreatedAt()); return m; }
    private Map<String, Object> redemptionMap(RewardRedemption r) { Map<String,Object> m=new LinkedHashMap<>(); m.put("id",r.getId()); m.put("rewardId",r.getReward().getId()); m.put("title",r.getReward().getTitle()); m.put("pointsCost",r.getPointsCost()); m.put("status",r.getStatus()); m.put("createdAt",r.getCreatedAt()); return m; }
}
