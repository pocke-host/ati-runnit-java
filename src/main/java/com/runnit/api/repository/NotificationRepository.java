package com.runnit.api.repository;

import com.runnit.api.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.actor WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    long countByUser_IdAndReadFalse(Long userId);

    boolean existsByUser_IdAndTypeAndReferenceTypeAndReadFalse(Long userId, String type, String referenceType);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId AND n.type = :type")
    void deleteByUserIdAndType(@Param("userId") Long userId, @Param("type") String type);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllReadByUserId(Long userId);
}
