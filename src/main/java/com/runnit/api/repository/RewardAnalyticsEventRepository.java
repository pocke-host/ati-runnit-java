package com.runnit.api.repository;
import com.runnit.api.model.RewardAnalyticsEvent;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RewardAnalyticsEventRepository extends JpaRepository<RewardAnalyticsEvent, Long> { long countByEventType(String eventType); }
