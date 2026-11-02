package com.veggiepal.ai.prompt;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {
    Optional<PromptVersion> findByKeyAndVersion(String key, int version);
    List<PromptVersion> findByKeyOrderByVersionDesc(String key);
}
