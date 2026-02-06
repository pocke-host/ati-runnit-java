// ========== ReactionService.java ==========
package com.runnit.api.service;

import com.runnit.api.model.Reaction;
import com.runnit.api.repository.ReactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReactionService {
    
    private final ReactionRepository reactionRepository;
    
    @Transactional
    public void addOrUpdateReaction(Long userId, Long momentId, Reaction.ReactionType type) {
        // Replace existing reaction (one per user per moment)
        reactionRepository.findByMomentIdAndUserId(momentId, userId)
                .ifPresentOrElse(
                    reaction -> {
                        reaction.setType(type);
                        reactionRepository.save(reaction);
                    },
                    () -> {
                        Reaction reaction = Reaction.builder()
                                .userId(userId)
                                .momentId(momentId)
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
