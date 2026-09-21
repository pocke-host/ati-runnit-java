package com.runnit.api.repository;
import com.runnit.api.model.RewardLedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface RewardLedgerRepository extends JpaRepository<RewardLedgerEntry, Long> {
    boolean existsByUserIdAndEventKey(Long userId, String eventKey);
    @Query("SELECT COALESCE(SUM(e.pointsDelta), 0) FROM RewardLedgerEntry e WHERE e.user.id = :userId") long balance(Long userId);
    List<RewardLedgerEntry> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}
