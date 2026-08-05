package com.runnit.api.repository;

import com.runnit.api.model.StrengthSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StrengthSetRepository extends JpaRepository<StrengthSet, Long> {
}
