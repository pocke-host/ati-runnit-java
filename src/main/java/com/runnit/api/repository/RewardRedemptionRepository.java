package com.runnit.api.repository;
import com.runnit.api.model.RewardRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {
    List<RewardRedemption> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<RewardRedemption> findTop100ByOrderByCreatedAtDesc();
    List<RewardRedemption> findByStatusOrderByCreatedAtAsc(String status);
    Optional<RewardRedemption> findByExternalCheckoutSessionId(String sessionId);
}
