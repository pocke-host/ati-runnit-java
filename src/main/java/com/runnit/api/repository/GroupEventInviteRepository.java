package com.runnit.api.repository;

import com.runnit.api.model.GroupEventInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface GroupEventInviteRepository extends JpaRepository<GroupEventInvite, Long> {
    List<GroupEventInvite> findByEventId(Long eventId);
    List<GroupEventInvite> findByInviteeIdAndStatusNot(Long inviteeId, String status);
    Optional<GroupEventInvite> findByEventIdAndInviteeId(Long eventId, Long inviteeId);
}
