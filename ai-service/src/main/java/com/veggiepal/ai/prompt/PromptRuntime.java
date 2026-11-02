package com.veggiepal.ai.prompt;

import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PromptRuntime {
    private final PromptDefinitionRepository definitions;
    private final PromptVersionRepository versions;

    public PromptRuntime(PromptDefinitionRepository definitions, PromptVersionRepository versions) {
        this.definitions = definitions;
        this.versions = versions;
    }

    public String activeContent(PromptKey key) {
        return resolve(key).content();
    }

    public ResolvedPrompt resolve(PromptKey key) {
        Integer active = definitions.findById(key.name())
                .orElseThrow(() -> new IllegalStateException("Missing prompt definition: " + key))
                .getActiveVersion();
        if (active == null || active == 0) return new ResolvedPrompt(0, key.defaultContent());
        String content = versions.findByKeyAndVersion(key.name(), active)
                .orElseThrow(() -> new IllegalStateException("Missing active prompt version: " + key + " v" + active))
                .getContent();
        return new ResolvedPrompt(active, content);
    }

    public String render(PromptKey key, Map<String, ?> variables) {
        return PromptTemplate.render(key, activeContent(key), variables);
    }

    public record ResolvedPrompt(int version, String content) { }
}
