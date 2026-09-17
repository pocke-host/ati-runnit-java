package com.runnit.api.repository;
import com.runnit.api.model.StripeWebhookEvent; import org.springframework.data.jpa.repository.JpaRepository;
public interface StripeWebhookEventRepository extends JpaRepository<StripeWebhookEvent,String> {}
