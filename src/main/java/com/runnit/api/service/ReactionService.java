package com.runnit.api.service;

import com.runnit.api.model.Moment;
import com.runnit.api.model.Reaction;
import com.runnit.api.model.User;
import com.runnit.api.repository.MomentRepository;
import com.runnit.api.repository.ReactionRepository;
import com.runnit.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReactionService {

    private final ReactionRepository reactionRepository;
    private final UserRepository userRepository;
    private final MomentRepository momentRepository;

    @Transactional
    public void addOrUpdateReaction(Long userId, Long momentId, Reaction.ReactionType type) {
        reactionRepository.findByMomentIdAndUserId(momentId, userId)
                .ifPresentOrElse(
                    reaction -> {
                        reaction.setType(type);
                        reactionRepository.save(reaction);
                    },
                    () -> {
                        User user = userRepository.findById(userId)
                                .orElseThrow(() -> new RuntimeException("User not found"));
                        Moment moment = momentRepository.findById(momentId)
                                .orElseThrow(() -> new RuntimeException("Moment not found"));
                        Reaction reaction = Reaction.builder()
                                .user(user)
                                .moment(moment)
                                .type(type)
                                .build();
                        reactionRepository.save(reaction);
                    }
                );
    }

    @Transactional
    public void removeReaction(Long userId, Long momentId) {
        reactionRepository.deleteByMomentIdAndUserId(momentId, userId);
    }
}
