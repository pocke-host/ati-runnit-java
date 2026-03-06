package com.runnit.api.dto;

import com.runnit.api.model.Reaction;
import jakarta.validation.constraints.NotNull;

public class ReactionRequest {

    @NotNull(message = "Reaction type is required")
    private Reaction.ReactionType type;

    public ReactionRequest() {}

    public Reaction.ReactionType getType() { return type; }
    public void setType(Reaction.ReactionType type) { this.type = type; }
}
