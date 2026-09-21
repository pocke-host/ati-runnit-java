package com.runnit.api.repository;
import com.runnit.api.model.RewardRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> { List<RewardRedemption> findByUserIdOrderByCreatedAtDesc(Long userId); }
