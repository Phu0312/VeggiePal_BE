package com.veggiepal.blog.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.enums.ContentStatus;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    Page<Blog> findByAuthorId(Long authorId, Pageable pageable);

    Page<Blog> findByAuthorIdAndStatus(Long authorId, ContentStatus status, Pageable pageable);

    Optional<Blog> findByIdAndStatus(Long id, ContentStatus status);

    boolean existsByCategoryId(Long categoryId);
}
