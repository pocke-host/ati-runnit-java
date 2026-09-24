package com.runnit.api.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;

@Entity @Table(name = "coach_reports")
public class CoachReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="reporter_id", nullable=false) private Long reporterId;
    @Column(name="coach_id", nullable=false) private Long coachId;
    @Column(name="booking_id") private Long bookingId;
    @Column(nullable=false, length=60) private String reason;
    @Column(length=2000) private String details;
    @Column(nullable=false, length=20) private String status = "OPEN";
    @CreationTimestamp @Column(name="created_at", updatable=false) private Instant createdAt;
    public CoachReport() {}
    public Long getId(){return id;} public Long getReporterId(){return reporterId;} public Long getCoachId(){return coachId;} public Long getBookingId(){return bookingId;} public String getReason(){return reason;} public String getDetails(){return details;} public String getStatus(){return status;} public Instant getCreatedAt(){return createdAt;}
    public void setReporterId(Long v){reporterId=v;} public void setCoachId(Long v){coachId=v;} public void setBookingId(Long v){bookingId=v;} public void setReason(String v){reason=v;} public void setDetails(String v){details=v;} public void setStatus(String v){status=v;}
}
