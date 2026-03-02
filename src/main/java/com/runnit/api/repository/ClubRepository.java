package com.runnit.api.repository;

import com.runnit.api.model.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    List<Club> findAllByOrderByCreatedAtDesc();

    @Query("SELECT c FROM Club c JOIN ClubMember cm ON c.id = cm.clubId WHERE cm.userId = :userId")
    List<Club> findByMemberUserId(Long userId);
}
