package com.veggiepal.ai.video;

import java.util.Optional;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface VideoJobRepository extends JpaRepository<VideoJob,Long>{
    Optional<VideoJob> findByIdAndUserId(Long id,Long userId);
    Optional<VideoJob> findBySourceVideoId(Long sourceVideoId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from VideoJob j where j.sourceVideoId=:sourceVideoId")
    Optional<VideoJob> findBySourceVideoIdForUpdate(@Param("sourceVideoId") Long sourceVideoId);
    Page<VideoJob> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    List<VideoJob> findByStatus(String status);
    List<VideoJob> findTop100ByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(String status, LocalDateTime cutoff);
    @Query("select j from VideoJob j where j.status <> :status and j.updatedAt < :cutoff " +
            "and (j.transcript is not null or j.extractionJson is not null) order by j.updatedAt asc")
    List<VideoJob> findPurgeCandidates(@Param("status") String status,
                                       @Param("cutoff") LocalDateTime cutoff, Pageable pageable);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from VideoJob j where j.id=:id")
    Optional<VideoJob> findForUpdate(@Param("id") Long id);
}
