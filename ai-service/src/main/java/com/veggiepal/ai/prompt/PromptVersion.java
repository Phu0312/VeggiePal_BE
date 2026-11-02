package com.veggiepal.ai.prompt;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "ai_prompt_versions", uniqueConstraints =
        @UniqueConstraint(name = "uk_prompt_key_version", columnNames = {"prompt_key", "version"}))
public class PromptVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "prompt_key", nullable = false, length = 64)
    private String key;
    @Column(nullable = false)
    private int version;
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "published_by")
    private Long publishedBy;
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    protected PromptVersion() { }
    public PromptVersion(PromptKey key, int version, String content, Long adminId) {
        this.key = key.name();
        this.version = version;
        this.content = content;
        this.status = "DRAFT";
        this.createdBy = adminId;
        this.createdAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public String getKey() { return key; }
    public int getVersion() { return version; }
    public String getContent() { return content; }
    public String getStatus() { return status; }
    public Long getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getPublishedBy() { return publishedBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void edit(String newContent) {
        if (!"DRAFT".equals(status)) throw new IllegalStateException("Published prompt is immutable");
        content = newContent;
    }
    public void publish(Long adminId) {
        if (!"DRAFT".equals(status)) throw new IllegalStateException("Prompt is not a draft");
        status = "PUBLISHED";
        publishedBy = adminId;
        publishedAt = LocalDateTime.now();
    }
}
