package com.runnit.api.repository;

import com.runnit.api.model.MentorshipMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MentorshipMatchRepository extends JpaRepository<MentorshipMatch, Long> {
    List<MentorshipMatch> findByMenteeId(Long menteeId);
    List<MentorshipMatch> findByMentorId(Long mentorId);
    Optional<MentorshipMatch> findByMentorIdAndMenteeId(Long mentorId, Long menteeId);
    boolean existsByMentorIdAndMenteeId(Long mentorId, Long menteeId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MentorshipMatch m WHERE m.mentor.id = :mentorId AND m.mentee.id = :menteeId")
    boolean hasExistingMatch(@Param("mentorId") Long mentorId, @Param("menteeId") Long menteeId);
}
