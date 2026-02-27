package com.runnit.api.repository;

import com.runnit.api.model.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    @Query("SELECT cr FROM ChatRoom cr JOIN ChatRoomMember crm ON crm.room = cr WHERE crm.user.id = :userId ORDER BY cr.createdAt DESC")
    List<ChatRoom> findRoomsForUser(@Param("userId") Long userId);

    @Query("SELECT cr FROM ChatRoom cr WHERE cr.roomType = 'DIRECT' AND cr.id IN " +
           "(SELECT crm1.room.id FROM ChatRoomMember crm1 WHERE crm1.user.id = :userId1) AND cr.id IN " +
           "(SELECT crm2.room.id FROM ChatRoomMember crm2 WHERE crm2.user.id = :userId2)")
    List<ChatRoom> findDirectRoom(@Param("userId1") Long userId1, @Param("userId2") Long userId2);
}
