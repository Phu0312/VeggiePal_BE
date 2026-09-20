package com.veggiepal.blog.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.Comment;
import com.veggiepal.blog.enums.CommentStatus;
import com.veggiepal.blog.enums.TargetType;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByTargetTypeAndTargetIdAndParentIsNullAndStatusIn(
            TargetType targetType, Long targetId, Collection<CommentStatus> statuses, Pageable pageable
    );

    Page<Comment> findByParentIdAndStatusIn(
            Long parentId, Collection<CommentStatus> statuses, Pageable pageable
    );

    /** One grouped query instead of a count per comment. */
    @Query("""
            select c.parent.id as parentId, count(c) as total
            from Comment c
            where c.parent.id in :parentIds and c.status in :statuses
            group by c.parent.id
            """)
    List<ReplyCount> countRepliesByParentIds(
            @Param("parentIds") Collection<Long> parentIds,
            @Param("statuses") Collection<CommentStatus> statuses
    );

    interface ReplyCount {
        Long getParentId();

        Long getTotal();
    }
}
