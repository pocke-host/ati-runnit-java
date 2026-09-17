package com.runnit.api.repository;
import com.runnit.api.model.CoachService; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface CoachServiceRepository extends JpaRepository<CoachService, Long> { List<CoachService> findByCoachIdAndActiveTrue(Long coachId); List<CoachService> findByCoachId(Long coachId); }
