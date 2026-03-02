package com.runnit.api.repository;

import com.runnit.api.model.ClubMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubMessageRepository extends JpaRepository<ClubMessage, Long> {
    List<ClubMessage> findByClubIdOrderByCreatedAtAsc(Long clubId);
}
