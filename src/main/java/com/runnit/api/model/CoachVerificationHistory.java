package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity
@Table(name = "coach_verification_history")
public class CoachVerificationHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "coach_id", nullable = false) private Long coachId;
    @Column(name = "admin_id", nullable = false) private Long adminId;
    @Column(nullable = false) private boolean verified;
    @Column(nullable = false, length = 40) private String action;
    @Column(length = 1000) private String note;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    public CoachVerificationHistory() {}
    public Long getId(){return id;} public Long getCoachId(){return coachId;} public Long getAdminId(){return adminId;}
    public boolean isVerified(){return verified;} public String getAction(){return action;} public String getNote(){return note;} public Instant getCreatedAt(){return createdAt;}
    public void setCoachId(Long v){coachId=v;} public void setAdminId(Long v){adminId=v;} public void setVerified(boolean v){verified=v;}
    public void setAction(String v){action=v;} public void setNote(String v){note=v;}
}
