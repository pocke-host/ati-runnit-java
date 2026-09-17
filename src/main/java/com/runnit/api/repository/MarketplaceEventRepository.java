package com.runnit.api.repository;
import com.runnit.api.model.MarketplaceEvent; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface MarketplaceEventRepository extends JpaRepository<MarketplaceEvent,Long>{ List<MarketplaceEvent> findByCoachId(Long coachId); }
