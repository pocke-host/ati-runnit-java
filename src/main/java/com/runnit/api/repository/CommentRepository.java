package com.runnit.api.repository;

import com.runnit.api.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByMomentIdOrderByCreatedAtAsc(Long momentId);
    List<Comment> findByActivityIdOrderByCreatedAtAsc(Long activityId);
    long countByMomentId(Long momentId);
    long countByActivityId(Long activityId);
}
