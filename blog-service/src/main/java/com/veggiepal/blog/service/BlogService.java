package com.veggiepal.blog.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.BlogMapper;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.repository.BlogRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogService {

    static final int MAX_PAGE_SIZE = 100;

    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    BlogRepository blogRepository;
    CategoryService categoryService;
    ContentModerationService contentModerationService;
    BlogMapper blogMapper;

    @Transactional
    public BlogResponse createBlog(Long authorId, BlogRequest request) {

        Blog blog = Blog.builder()
                .authorId(authorId)
                .category(categoryService.requireActiveCategory(request.getCategoryId()))
                .title(request.getTitle().trim())
                .content(request.getContent())
                .status(ContentStatus.DRAFT)
                .viewCount(0)
                .voteScore(0)
                .build();

        String reason = Boolean.TRUE.equals(request.getPublish())
                ? applyModeration(blog)
                : null;

        blogRepository.save(blog);
        return withReason(blog, reason);
    }

    @Transactional
    public BlogResponse updateBlog(Long userId, boolean admin, Long blogId, BlogRequest request) {

        Blog blog = findOwnedBlog(userId, admin, blogId);

        blog.setCategory(categoryService.requireActiveCategory(request.getCategoryId()));
        blog.setTitle(request.getTitle().trim());
        blog.setContent(request.getContent());

        // A draft is not public yet, so editing it needs no moderation. Anything
        // else has been (or is being) considered already and must be reconsidered.
        String reason = blog.getStatus() == ContentStatus.DRAFT
                ? null
                : applyModeration(blog);

        blogRepository.save(blog);
        return withReason(blog, reason);
    }

    @Transactional
    public BlogResponse submitBlog(Long userId, Long blogId) {

        Blog blog = findOwnedBlog(userId, false, blogId);

        if (blog.getStatus() != ContentStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_BLOG_STATUS_TRANSITION);
        }

        String reason = applyModeration(blog);

        blogRepository.save(blog);
        return withReason(blog, reason);
    }

    @Transactional
    public void deleteBlog(Long userId, boolean admin, Long blogId) {

        blogRepository.delete(findOwnedBlog(userId, admin, blogId));
    }

    public PageResponse<BlogSummaryResponse> getOwnBlogs(
            Long authorId, ContentStatus status, int page, int size
    ) {

        PageRequest pageRequest = pageRequest(page, size, NEWEST_FIRST);

        Page<Blog> blogs = status == null
                ? blogRepository.findByAuthorId(authorId, pageRequest)
                : blogRepository.findByAuthorIdAndStatus(authorId, status, pageRequest);

        return toPageResponse(blogs);
    }

    /** Used by CommentService and VoteService: only published content can be interacted with. */
    public Blog requirePublishedBlog(Long blogId) {

        return blogRepository
                .findByIdAndStatus(blogId, ContentStatus.PUBLISHED)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.BLOG_NOT_EXISTED
                        )
                );
    }

    /**
     * BR-02. Returns the moderation reason, or null when the content was approved.
     * Mutates the blog's status in place.
     */
    private String applyModeration(Blog blog) {

        ModerationResult result = contentModerationService.moderate(
                blog.getTitle() + "\n" + blog.getContent()
        );

        switch (result.decision()) {

            case APPROVED -> {
                blog.setStatus(ContentStatus.PUBLISHED);

                // Stamped once: re-publishing an edited post must not reshuffle the feed
                if (blog.getPublishedAt() == null) {
                    blog.setPublishedAt(LocalDateTime.now());
                }

                return null;
            }

            case REJECTED -> blog.setStatus(ContentStatus.REJECTED);

            case PENDING -> blog.setStatus(ContentStatus.PENDING);
        }

        return result.reason();
    }

    private Blog findOwnedBlog(Long userId, boolean admin, Long blogId) {

        Blog blog = blogRepository
                .findById(blogId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.BLOG_NOT_EXISTED
                        )
                );

        // BR-07: the owner, or an admin taking down a violation (FR-10-04)
        if (!admin && !blog.getAuthorId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return blog;
    }

    private BlogResponse withReason(Blog blog, String reason) {

        BlogResponse response = blogMapper.toBlogResponse(blog);
        response.setModerationReason(reason);
        return response;
    }

    static PageRequest pageRequest(int page, int size, Sort sort) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.min(Math.max(page, 0), Integer.MAX_VALUE / safeSize);

        return PageRequest.of(safePage, safeSize, sort);
    }

    PageResponse<BlogSummaryResponse> toPageResponse(Page<Blog> blogs) {

        return PageResponse.<BlogSummaryResponse>builder()
                .items(blogs.getContent().stream().map(blogMapper::toBlogSummaryResponse).toList())
                .page(blogs.getNumber())
                .size(blogs.getSize())
                .totalElements(blogs.getTotalElements())
                .totalPages(blogs.getTotalPages())
                .build();
    }
}
