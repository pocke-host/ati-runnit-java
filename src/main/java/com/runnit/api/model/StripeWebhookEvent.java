package com.runnit.api.model;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="stripe_webhook_events") public class StripeWebhookEvent {
 @Id @Column(name="event_id") private String eventId; @Column(name="event_type",nullable=false) private String eventType; @Column(name="received_at",nullable=false) private Instant receivedAt=Instant.now();
 public StripeWebhookEvent(){} public StripeWebhookEvent(String id,String type){eventId=id;eventType=type;} public String getEventId(){return eventId;}
}
