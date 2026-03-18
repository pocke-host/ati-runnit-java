package com.runnit.api.repository;

import com.runnit.api.model.LiveShare;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LiveShareRepository extends JpaRepository<LiveShare, Long> {
    Optional<LiveShare> findByToken(String token);
    Optional<LiveShare> findByTokenAndUserId(String token, Long userId);
}
