package com.runnit.api.repository;

import com.runnit.api.model.InviteEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InviteEventRepository extends JpaRepository<InviteEvent, Long> {

    long countByInviter_IdAndEventType(Long inviterId, String eventType);

    long countByInviter_IdAndEventTypeAndVisitorIsNull(Long inviterId, String eventType);
}
