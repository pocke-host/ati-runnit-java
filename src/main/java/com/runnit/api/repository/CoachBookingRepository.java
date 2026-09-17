package com.runnit.api.repository;
import com.runnit.api.model.CoachBooking; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface CoachBookingRepository extends JpaRepository<CoachBooking, Long> { List<CoachBooking> findByCoachIdOrderByCreatedAtDesc(Long coachId); List<CoachBooking> findByAthleteIdOrderByCreatedAtDesc(Long athleteId); }
