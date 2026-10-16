package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotNull;

public record ModerationDecisionRequest(
        @NotNull Decision decision,
        @NotNull Long authorId
) {
    public enum Decision { APPROVED, REJECTED }
}
