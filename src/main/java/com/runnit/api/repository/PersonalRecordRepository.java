package com.runnit.api.repository;

import com.runnit.api.model.PersonalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonalRecordRepository extends JpaRepository<PersonalRecord, Long> {
    Optional<PersonalRecord> findByUserId(Long userId);
}
