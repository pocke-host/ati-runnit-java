package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;

@Entity
@Table(name = "coach_services")
public class CoachService {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "coach_id", nullable = false) private Long coachId;
    @Column(nullable = false, length = 160) private String title;
    @Column(columnDefinition = "TEXT") private String description;
    @Column(name = "service_type", nullable = false, length = 30) private String serviceType;
    @Column(name = "billing_type", nullable = false, length = 20) private String billingType;
    @Column(name = "price_cents", nullable = false) private Integer priceCents;
    @Column(name = "duration_minutes") private Integer durationMinutes;
    @Column(nullable = false) private Boolean active = true;
    @CreationTimestamp @Column(name = "created_at", updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;
    public CoachService() {}
    public Long getId() { return id; } public Long getCoachId() { return coachId; } public String getTitle() { return title; }
    public String getDescription() { return description; } public String getServiceType() { return serviceType; } public String getBillingType() { return billingType; }
    public Integer getPriceCents() { return priceCents; } public Integer getDurationMinutes() { return durationMinutes; } public Boolean getActive() { return active; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
    public void setCoachId(Long v) { coachId = v; } public void setTitle(String v) { title = v; } public void setDescription(String v) { description = v; }
    public void setServiceType(String v) { serviceType = v; } public void setBillingType(String v) { billingType = v; } public void setPriceCents(Integer v) { priceCents = v; }
    public void setDurationMinutes(Integer v) { durationMinutes = v; } public void setActive(Boolean v) { active = v; }
}
