package com.runnit.api.repository;
import com.runnit.api.model.CoachReview; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List; import java.util.Optional;
public interface CoachReviewRepository extends JpaRepository<CoachReview, Long> { List<CoachReview> findByCoachIdOrderByCreatedAtDesc(Long coachId); Optional<CoachReview> findByBookingId(Long bookingId); }
