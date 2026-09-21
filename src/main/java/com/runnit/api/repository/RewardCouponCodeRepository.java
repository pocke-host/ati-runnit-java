package com.runnit.api.repository;
import com.runnit.api.model.RewardCouponCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface RewardCouponCodeRepository extends JpaRepository<RewardCouponCode, Long> { List<RewardCouponCode> findByRewardIdOrderByIdDesc(Long rewardId); Optional<RewardCouponCode> findByCodeIgnoreCase(String code); }
