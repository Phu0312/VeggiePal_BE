package com.veggiepal.ai.prompt;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_prompt_definitions")
public class PromptDefinition {
    @Id
    @Column(name = "prompt_key", length = 64)
    private String key;

    @Column(name = "active_version")
    private Integer activeVersion;

    @Column(name = "next_version", nullable = false)
    private int nextVersion;

    protected PromptDefinition() { }

    public String getKey() { return key; }
    public Integer getActiveVersion() { return activeVersion; }
    public int allocateVersion() { return nextVersion++; }
    public void activate(int version) { activeVersion = version; }
}
