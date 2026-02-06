// ========== ReactionRequest.java ==========
package com.runnit.api.dto;

import com.runnit.api.model.Reaction;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReactionRequest {
    
    @NotNull(message = "Reaction type is required")
    private Reaction.ReactionType type;
}