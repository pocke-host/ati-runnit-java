package com.runnit.api.repository;
import com.runnit.api.model.CoachReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CoachReportRepository extends JpaRepository<CoachReport, Long> { List<CoachReport> findByCoachIdOrderByCreatedAtDesc(Long coachId); }
