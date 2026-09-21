package com.runnit.api.repository;
import com.runnit.api.model.RewardCatalogItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RewardCatalogRepository extends JpaRepository<RewardCatalogItem, Long> { List<RewardCatalogItem> findByActiveTrueOrderByPointsCostAsc(); }
