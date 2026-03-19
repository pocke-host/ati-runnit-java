package com.runnit.api.repository;

import com.runnit.api.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByMomentIdOrderByCreatedAtAsc(Long momentId);
    List<Comment> findByActivityIdOrderByCreatedAtAsc(Long activityId);
    long countByMomentId(Long momentId);
    long countByActivityId(Long activityId);

    @Query("SELECT c.activity.id, COUNT(c) FROM Comment c WHERE c.activity.id IN :ids GROUP BY c.activity.id")
    List<Object[]> countGroupedByActivityIds(@Param("ids") List<Long> ids);
}
