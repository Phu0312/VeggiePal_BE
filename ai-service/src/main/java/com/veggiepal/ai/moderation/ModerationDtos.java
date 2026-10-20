package com.veggiepal.ai.moderation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ModerationDtos {
    private ModerationDtos() {}
    public enum ContentType { BLOG, VIDEO, COMMENT }
    public enum Decision { APPROVED, REJECTED, REVIEW_REQUIRED, DISMISSED }
    public record ModerationRequest(@NotNull ContentType contentType, Long externalContentId, Long authorId,
                                    @NotBlank @Size(max=20000) String text) {}
    public record ReviewRequest(@NotNull Decision decision, @Size(max=1000) String reason) {}
    public record ModerationResponse(Long caseId, ContentType contentType, Long externalContentId,
                                     Long authorId, String content, Decision decision,
                                     List<String> categories, String reason, BigDecimal confidence,
                                     Long reviewedBy, LocalDateTime reviewedAt, LocalDateTime createdAt) {}
}
