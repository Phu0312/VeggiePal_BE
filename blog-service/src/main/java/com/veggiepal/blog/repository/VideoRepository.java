package com.veggiepal.blog.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.Video;
import com.veggiepal.blog.enums.ContentStatus;
import jakarta.persistence.LockModeType;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {

    @Query("""
            select v from Video v
            where (:status is null or v.status = :status)
              and (:authorId is null or v.authorId = :authorId)
              and (:keyword is null or lower(v.title) like concat('%', :keyword, '%')
                   or lower(v.description) like concat('%', :keyword, '%'))
            """)
    Page<Video> adminSearch(@Param("status") ContentStatus status, @Param("authorId") Long authorId,
                            @Param("keyword") String keyword, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Video v where v.id = :id")
    Optional<Video> findForModeration(@Param("id") Long id);

    Page<Video> findByStatus(ContentStatus status, Pageable pageable);

    Page<Video> findByStatusAndCategoryId(ContentStatus status, Long categoryId, Pageable pageable);

    Page<Video> findByAuthorId(Long authorId, Pageable pageable);

    Page<Video> findByAuthorIdAndStatus(Long authorId, ContentStatus status, Pageable pageable);

    Optional<Video> findByIdAndStatus(Long id, ContentStatus status);

    boolean existsByCategoryId(Long categoryId);

    /** Atomic, like BlogRepository.incrementViewCount: concurrent readers must not lose views. */
    @Modifying
    @Query("update Video v set v.viewCount = v.viewCount + 1 where v.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /** Atomic, like BlogRepository.addVoteScore. */
    @Modifying
    @Query("update Video v set v.voteScore = v.voteScore + :delta where v.id = :id")
    void addVoteScore(@Param("id") Long id, @Param("delta") int delta);
}
