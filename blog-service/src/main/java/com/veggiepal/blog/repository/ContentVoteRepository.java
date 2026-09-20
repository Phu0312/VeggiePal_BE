package com.veggiepal.blog.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.ContentVote;
import com.veggiepal.blog.enums.TargetType;

@Repository
public interface ContentVoteRepository extends JpaRepository<ContentVote, Long> {

    Optional<ContentVote> findByUserIdAndTargetTypeAndTargetId(
            Long userId, TargetType targetType, Long targetId
    );

    List<ContentVote> findByUserIdAndTargetTypeAndTargetIdIn(
            Long userId, TargetType targetType, Collection<Long> targetIds
    );
}
