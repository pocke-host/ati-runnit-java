package com.runnit.api.repository;

import com.runnit.api.model.SosEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SosEventRepository extends JpaRepository<SosEvent, Long> {
}
