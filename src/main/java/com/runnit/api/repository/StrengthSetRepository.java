package com.runnit.api.repository;

import com.runnit.api.model.StrengthSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StrengthSetRepository extends JpaRepository<StrengthSet, Long> {
    List<StrengthSet> findByExerciseIdInOrderBySetNumberAsc(List<Long> exerciseIds);
}
