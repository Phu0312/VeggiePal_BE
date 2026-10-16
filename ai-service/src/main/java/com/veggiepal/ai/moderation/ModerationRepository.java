package com.veggiepal.ai.moderation;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationRepository extends JpaRepository<ModerationCase,Long> {
    List<ModerationCase> findByDecisionOrderByCreatedAtAsc(String decision);
}
