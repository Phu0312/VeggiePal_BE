package com.veggiepal.ai.moderation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="ai_moderation_cases")
@Getter
@NoArgsConstructor
public class ModerationCase {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="content_type", nullable=false, length=30) private String contentType;
    @Column(name="external_content_id") private Long externalContentId;
    @Column(name="author_id") private Long authorId;
    @Column(name="content_text", columnDefinition="TEXT") private String contentText;
    @Column(nullable=false, length=30) private String decision;
    @Column(length=500) private String categories;
    @Column(length=1000) private String reason;
    @Column(nullable=false, precision=5, scale=4) private BigDecimal confidence;
    @Column(name="reviewed_by") private Long reviewedBy;
    @Column(name="reviewed_at") private LocalDateTime reviewedAt;
    @Column(name="created_at", nullable=false) private LocalDateTime createdAt;

    public ModerationCase(String type, Long externalId, Long authorId, String contentText,
            String decision, String categories, String reason, BigDecimal confidence) {
        this.contentType=type; this.externalContentId=externalId; this.authorId=authorId; this.decision=decision;
        this.contentText=contentText; this.categories=categories; this.reason=reason;
        this.confidence=confidence; this.createdAt=LocalDateTime.now();
    }
    public void refresh(String contentText, String decision, String categories, String reason, BigDecimal confidence) {
        this.contentText=contentText; this.decision=decision; this.categories=categories;
        this.reason=reason; this.confidence=confidence; this.reviewedBy=null; this.reviewedAt=null;
    }
    public void review(Long adminId, String decision, String reason) {
        this.decision=decision; this.reason=reason; this.reviewedBy=adminId; this.reviewedAt=LocalDateTime.now();
    }
    public boolean isPending() {
        return "REVIEW_REQUIRED".equals(decision) && reviewedAt == null;
    }
}
