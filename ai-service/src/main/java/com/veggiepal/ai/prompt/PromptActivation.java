package com.veggiepal.ai.prompt;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_prompt_activations")
public class PromptActivation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "prompt_key", nullable = false, length = 64)
    private String key;
    @Column(nullable = false)
    private int version;
    @Column(name = "activated_by", nullable = false)
    private Long activatedBy;
    @Column(name = "activated_at", nullable = false)
    private LocalDateTime activatedAt;

    protected PromptActivation() { }
    public PromptActivation(PromptKey key, int version, Long adminId) {
        this.key = key.name(); this.version = version; this.activatedBy = adminId;
        this.activatedAt = LocalDateTime.now();
    }
    public Long getId() { return id; }
    public String getKey() { return key; }
    public int getVersion() { return version; }
    public Long getActivatedBy() { return activatedBy; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
}
