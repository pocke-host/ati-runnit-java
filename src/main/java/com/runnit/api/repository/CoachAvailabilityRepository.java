package com.runnit.api.repository;
import com.runnit.api.model.CoachAvailability; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface CoachAvailabilityRepository extends JpaRepository<CoachAvailability, Long> { List<CoachAvailability> findByCoachIdOrderByWeekdayAscStartTimeAsc(Long coachId); }
