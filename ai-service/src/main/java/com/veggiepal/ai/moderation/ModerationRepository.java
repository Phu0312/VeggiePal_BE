package com.veggiepal.ai.moderation;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ModerationRepository extends JpaRepository<ModerationCase,Long>, JpaSpecificationExecutor<ModerationCase> {
    List<ModerationCase> findByDecisionOrderByCreatedAtAsc(String decision);
    Optional<ModerationCase> findFirstByContentTypeAndExternalContentIdAndAuthorIdAndReviewedAtIsNullOrderByCreatedAtDesc(
            String contentType, Long externalContentId, Long authorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from ModerationCase m where m.id = :id")
    Optional<ModerationCase> findForReview(@Param("id") Long id);
}
