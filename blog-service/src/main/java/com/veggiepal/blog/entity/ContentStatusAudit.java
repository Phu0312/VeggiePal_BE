package com.veggiepal.blog.entity;

import com.veggiepal.blog.enums.ContentStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "content_status_audits", indexes =
        @Index(name = "idx_content_status_audits_target", columnList = "content_type, content_id"))
@Getter
@NoArgsConstructor
public class ContentStatusAudit {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "content_type", nullable = false, length = 10)
    private String contentType;
    @Column(name = "content_id", nullable = false)
    private Long contentId;
    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false)
    private ContentStatus previousStatus;
    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false)
    private ContentStatus newStatus;
    @Column(nullable = false, length = 1000)
    private String reason;
    @Column(name = "reviewed_by", nullable = false)
    private Long reviewedBy;
    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    public ContentStatusAudit(String contentType, Long contentId, ContentStatus previousStatus,
                              ContentStatus newStatus, String reason, Long reviewedBy) {
        this.contentType = contentType;
        this.contentId = contentId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.reason = reason;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = LocalDateTime.now();
    }
}
