package com.veggiepal.ai.prompt;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PromptDefinitionRepository extends JpaRepository<PromptDefinition, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from PromptDefinition d where d.key = :key")
    Optional<PromptDefinition> findForUpdateByKey(@Param("key") String key);
}
