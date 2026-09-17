package com.runnit.api.model;
import jakarta.persistence.*; import org.hibernate.annotations.CreationTimestamp; import java.time.Instant;
@Entity @Table(name="coach_reviews")
public class CoachReview {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id; @Column(name="booking_id",nullable=false,unique=true) private Long bookingId; @Column(name="coach_id",nullable=false) private Long coachId; @Column(name="athlete_id",nullable=false) private Long athleteId; @Column(nullable=false) private Integer rating; @Column(name="review_text") private String reviewText; @CreationTimestamp @Column(name="created_at",updatable=false) private Instant createdAt;
 public CoachReview(){} public Long getId(){return id;} public Long getBookingId(){return bookingId;} public Long getCoachId(){return coachId;} public Long getAthleteId(){return athleteId;} public Integer getRating(){return rating;} public String getReviewText(){return reviewText;} public Instant getCreatedAt(){return createdAt;} public void setBookingId(Long v){bookingId=v;} public void setCoachId(Long v){coachId=v;} public void setAthleteId(Long v){athleteId=v;} public void setRating(Integer v){rating=v;} public void setReviewText(String v){reviewText=v;}
}
