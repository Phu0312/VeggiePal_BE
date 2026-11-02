package com.veggiepal.ai.prompt;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptActivationRepository extends JpaRepository<PromptActivation, Long> {
    List<PromptActivation> findByKeyOrderByActivatedAtDesc(String key);
}
