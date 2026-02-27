package com.runnit.api.service;

import com.runnit.api.dto.CommentRequest;
import com.runnit.api.dto.CommentResponse;
import com.runnit.api.model.Comment;
import com.runnit.api.model.Moment;
import com.runnit.api.model.User;
import com.runnit.api.repository.CommentRepository;
import com.runnit.api.repository.MomentRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final MomentRepository momentRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommentResponse addComment(Long momentId, Long userId, CommentRequest request) {
        Moment moment = momentRepository.findById(momentId)
                .orElseThrow(() -> new RuntimeException("Moment not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = Comment.builder()
                .moment(moment)
                .user(user)
                .content(request.getContent())
                .build();

        comment = commentRepository.save(comment);
        return toResponse(comment);
    }

    public Page<CommentResponse> getComments(Long momentId, int page, int size) {
        if (!momentRepository.existsById(momentId)) {
            throw new RuntimeException("Moment not found");
        }
        return commentRepository.findByMomentIdOrderByCreatedAtAsc(momentId, PageRequest.of(page, size))
                .map(this::toResponse);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this comment");
        }
        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .userId(comment.getUser().getId())
                .userDisplayName(comment.getUser().getDisplayName())
                .userAvatarUrl(comment.getUser().getAvatarUrl())
                .momentId(comment.getMoment().getId())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }
}
