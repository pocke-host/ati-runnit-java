package com.runnit.api.repository;
import com.runnit.api.model.RewardAbuseFlag;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RewardAbuseFlagRepository extends JpaRepository<RewardAbuseFlag, Long> { boolean existsByUserIdAndStatus(Long userId, String status); }
