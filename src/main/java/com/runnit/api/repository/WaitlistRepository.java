package com.runnit.api.repository;

import com.runnit.api.model.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository extends JpaRepository<WaitlistEntry, Long> {
    boolean existsByEmail(String email);
}
