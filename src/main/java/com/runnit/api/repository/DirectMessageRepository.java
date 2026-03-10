package com.runnit.api.repository;

import com.runnit.api.model.DirectMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Long> {

    // All messages between two users, chronological
    @Query("SELECT m FROM DirectMessage m WHERE (m.senderId = :a AND m.receiverId = :b) OR (m.senderId = :b AND m.receiverId = :a) ORDER BY m.createdAt ASC")
    List<DirectMessage> findConversation(@Param("a") Long userA, @Param("b") Long userB);

    // Distinct partner IDs for a user (sent or received)
    @Query("SELECT DISTINCT CASE WHEN m.senderId = :userId THEN m.receiverId ELSE m.senderId END FROM DirectMessage m WHERE m.senderId = :userId OR m.receiverId = :userId")
    List<Long> findConversationPartnerIds(@Param("userId") Long userId);

    // Latest message per conversation partner
    @Query("SELECT m FROM DirectMessage m WHERE m.id IN (" +
           "SELECT MAX(m2.id) FROM DirectMessage m2 WHERE m2.senderId = :userId OR m2.receiverId = :userId GROUP BY " +
           "CASE WHEN m2.senderId = :userId THEN m2.receiverId ELSE m2.senderId END)")
    List<DirectMessage> findLatestMessagePerConversation(@Param("userId") Long userId);

    long countBySenderIdAndReceiverIdAndReadFalse(Long senderId, Long receiverId);

    @Modifying
    @Transactional
    @Query("UPDATE DirectMessage m SET m.read = true WHERE m.senderId = :senderId AND m.receiverId = :receiverId AND m.read = false")
    void markConversationRead(@Param("senderId") Long senderId, @Param("receiverId") Long receiverId);
}
