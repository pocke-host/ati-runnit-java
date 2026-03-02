package com.runnit.api.repository;

import com.runnit.api.model.ClubMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClubMemberRepository extends JpaRepository<ClubMember, ClubMember.ClubMemberId> {
    List<ClubMember> findByClubId(Long clubId);
    boolean existsByClubIdAndUserId(Long clubId, Long userId);
    void deleteByClubIdAndUserId(Long clubId, Long userId);
}
