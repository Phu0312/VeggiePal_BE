package com.veggiepal.ai.prompt;

import com.veggiepal.ai.common.AppException;
import com.veggiepal.ai.common.ErrorCode;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PromptManagementService {
    private final PromptDefinitionRepository definitions;
    private final PromptVersionRepository versions;
    private final PromptRuntime runtime;
    private final PromptActivationRepository activations;

    public PromptManagementService(PromptDefinitionRepository definitions,
                                   PromptVersionRepository versions, PromptRuntime runtime,
                                   PromptActivationRepository activations) {
        this.definitions = definitions;
        this.versions = versions;
        this.runtime = runtime;
        this.activations = activations;
    }

    public List<PromptSummary> list() {
        return Arrays.stream(PromptKey.values()).map(this::summary).toList();
    }

    public PromptSummary summary(PromptKey key) {
        var definition = definitions.findById(key.name()).orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
        return new PromptSummary(key, key.title(), key.variables(),
                definition.getActiveVersion() == null ? 0 : definition.getActiveVersion(), runtime.activeContent(key));
    }

    public List<PromptVersionView> history(PromptKey key) {
        List<PromptVersionView> saved = versions.findByKeyOrderByVersionDesc(key.name()).stream()
                .map(PromptVersionView::of).toList();
        var all = new java.util.ArrayList<>(saved);
        all.add(new PromptVersionView(0, "DEFAULT", key.defaultContent(), null, null, null, null));
        return all;
    }

    public List<PromptActivation> activationHistory(PromptKey key) {
        return activations.findByKeyOrderByActivatedAtDesc(key.name());
    }

    @Transactional
    public PromptVersionView createDraft(PromptKey key, Long adminId, String content) {
        var definition = locked(key);
        String draft = content == null ? runtime.activeContent(key) : content;
        validate(key, draft);
        return PromptVersionView.of(versions.save(new PromptVersion(key, definition.allocateVersion(), draft, adminId)));
    }

    @Transactional
    public PromptVersionView editDraft(PromptKey key, int version, String content) {
        locked(key);
        var entry = version(key, version);
        if (!"DRAFT".equals(entry.getStatus())) throw new AppException(ErrorCode.INVALID_REQUEST, "Only drafts can be edited");
        validate(key, content);
        entry.edit(content);
        return PromptVersionView.of(entry);
    }

    @Transactional
    public PromptVersionView publish(PromptKey key, int version, Long adminId) {
        var definition = locked(key);
        var entry = version(key, version);
        if (!"DRAFT".equals(entry.getStatus())) throw new AppException(ErrorCode.INVALID_REQUEST, "Only drafts can be published");
        validate(key, entry.getContent());
        entry.publish(adminId);
        definition.activate(version);
        activations.save(new PromptActivation(key, version, adminId));
        return PromptVersionView.of(entry);
    }

    @Transactional
    public PromptSummary restore(PromptKey key, int version, Long adminId) {
        var definition = locked(key);
        if (version != 0 && !"PUBLISHED".equals(version(key, version).getStatus())) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Only published versions can be restored");
        }
        definition.activate(version == 0 ? 0 : version);
        activations.save(new PromptActivation(key, version, adminId));
        return new PromptSummary(key, key.title(), key.variables(), version,
                version == 0 ? key.defaultContent() : version(key, version).getContent());
    }

    public PromptVersionView getVersion(PromptKey key, int version) {
        if (version == 0) return new PromptVersionView(0, "DEFAULT", key.defaultContent(), null, null, null, null);
        return PromptVersionView.of(version(key, version));
    }

    private PromptDefinition locked(PromptKey key) {
        return definitions.findForUpdateByKey(key.name())
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private PromptVersion version(PromptKey key, int version) {
        return versions.findByKeyAndVersion(key.name(), version)
                .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void validate(PromptKey key, String content) {
        try { PromptTemplate.validate(key, content); }
        catch (IllegalArgumentException exception) { throw new AppException(ErrorCode.INVALID_REQUEST, exception.getMessage()); }
    }

    public record PromptSummary(PromptKey key, String title, java.util.Set<String> variables,
                                int activeVersion, String activeContent) { }

    public record PromptVersionView(int version, String status, String content, Long createdBy,
                                    java.time.LocalDateTime createdAt, Long publishedBy,
                                    java.time.LocalDateTime publishedAt) {
        static PromptVersionView of(PromptVersion entry) {
            return new PromptVersionView(entry.getVersion(), entry.getStatus(), entry.getContent(),
                    entry.getCreatedBy(), entry.getCreatedAt(), entry.getPublishedBy(), entry.getPublishedAt());
        }
    }
}
