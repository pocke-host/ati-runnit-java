package com.runnit.api.repository;

import com.runnit.api.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByMomentIdOrderByCreatedAtAsc(Long momentId, Pageable pageable);
    long countByMomentId(Long momentId);
    List<Comment> findByMomentIdOrderByCreatedAtAsc(Long momentId);
}
