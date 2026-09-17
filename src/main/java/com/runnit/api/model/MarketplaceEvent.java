package com.runnit.api.model;
import jakarta.persistence.*; import org.hibernate.annotations.CreationTimestamp; import java.time.Instant;
@Entity @Table(name="marketplace_events") public class MarketplaceEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="event_type",nullable=false) private String eventType; @Column(name="coach_id") private Long coachId; @Column(name="athlete_id") private Long athleteId; @Column(name="service_id") private Long serviceId; @Column(name="booking_id") private Long bookingId; @CreationTimestamp @Column(name="created_at",updatable=false) private Instant createdAt;
 public MarketplaceEvent(){} public MarketplaceEvent(String type,Long coach,Long athlete,Long service,Long booking){eventType=type;coachId=coach;athleteId=athlete;serviceId=service;bookingId=booking;} public String getEventType(){return eventType;} public Long getCoachId(){return coachId;} public Long getServiceId(){return serviceId;} public Long getBookingId(){return bookingId;}
}
