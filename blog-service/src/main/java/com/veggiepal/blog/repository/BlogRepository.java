package com.veggiepal.blog.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.enums.ContentStatus;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    Page<Blog> findByAuthorIdInAndStatus(java.util.Collection<Long> authorIds, ContentStatus status, Pageable pageable);

    Page<Blog> findByAuthorId(Long authorId, Pageable pageable);

    Page<Blog> findByAuthorIdAndStatus(Long authorId, ContentStatus status, Pageable pageable);

    Optional<Blog> findByIdAndStatus(Long id, ContentStatus status);

    boolean existsByCategoryId(Long categoryId);

    // Task sheet US7: title matches rank above body-only matches. The Pageable's sort is
    // appended after this expression, so it only orders rows within each tier. With no
    // keyword, like null is NULL and every row falls into the same tier.
    @Query("""
            select b from Blog b
            where b.status = :status
              and (:categoryId is null or b.category.id = :categoryId)
              and (:keyword is null
                   or lower(b.title) like lower(concat('%', :keyword, '%'))
                   or lower(b.content) like lower(concat('%', :keyword, '%')))
            order by case when lower(b.title) like lower(concat('%', :keyword, '%')) then 0 else 1 end
            """)
    Page<Blog> search(
            @Param("status") ContentStatus status,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<Blog> findByCategoryIdAndStatusAndIdNot(
            Long categoryId, ContentStatus status, Long id, Pageable pageable
    );

    /**
     * One atomic statement. Read-modify-write would let two concurrent readers
     * overwrite each other's increment and silently lose views.
     */
    @Modifying
    @Query("update Blog b set b.viewCount = b.viewCount + 1 where b.id = :id")
    void incrementViewCount(@Param("id") Long id);

    /** Atomic so two concurrent votes cannot overwrite each other's adjustment. */
    @Modifying
    @Query("update Blog b set b.voteScore = b.voteScore + :delta where b.id = :id")
    void addVoteScore(@Param("id") Long id, @Param("delta") int delta);
}
