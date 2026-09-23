package com.veggiepal.blog.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.enums.ImageType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.BlogMapper;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.repository.BlogRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class BlogService {

    static final int MAX_PAGE_SIZE = 100;

    static final long MAX_THUMBNAIL_BYTES = 5L * 1024 * 1024;

    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    static final Sort POPULAR_FIRST = Sort.by(Sort.Order.desc("voteScore"), Sort.Order.desc("id"));

    static final Sort MOST_VIEWED_FIRST = Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("id"));

    static final Sort RECENTLY_PUBLISHED_FIRST =
            Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));

    static final int RELATED_LIMIT = 5;

    BlogRepository blogRepository;
    CategoryService categoryService;
    ContentModerationService contentModerationService;
    BlogMapper blogMapper;
    FileStorageService fileStorageService;

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

        // Editing re-runs moderation, which approves; without this an owner could lift an
        // admin's ban just by saving the post again.
        if (blog.getStatus() == ContentStatus.BANNED) {
            throw new AppException(ErrorCode.INVALID_BLOG_STATUS_TRANSITION);
        }

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

        // Same invariant as createBlog and updateBlog: a draft must not go public
        // under a category that has been deactivated since it was written.
        categoryService.requireActiveCategory(blog.getCategory().getId());

        String reason = applyModeration(blog);

        blogRepository.save(blog);
        return withReason(blog, reason);
    }

    @Transactional
    public void deleteBlog(Long userId, boolean admin, Long blogId) {

        Blog blog = findOwnedBlog(userId, admin, blogId);

        // Only an admin gets past findOwnedBlog on someone else's post, and that is a
        // takedown: hide it but keep it, so the owner still sees it as banned (task sheet
        // US3/US6). An owner removing their own post — admin or not — is a real delete.
        if (!blog.getAuthorId().equals(userId)) {
            blog.setStatus(ContentStatus.BANNED);
            blogRepository.save(blog);
            return;
        }

        blogRepository.delete(blog);
    }

    @Transactional
    public BlogResponse uploadThumbnail(Long userId, boolean admin, Long blogId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.THUMBNAIL_REQUIRED);
        }

        if (file.getSize() > MAX_THUMBNAIL_BYTES) {
            throw new AppException(ErrorCode.THUMBNAIL_TOO_LARGE);
        }

        // Ownership before storage: a stranger must not be able to write into the bucket
        Blog blog = findOwnedBlog(userId, admin, blogId);

        byte[] content = readContent(file);

        // The declared content type must match the real file signature
        ImageType imageType = ImageTypeDetector
                .detect(content)
                .filter(type -> type.getContentType().equals(file.getContentType()))
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.INVALID_THUMBNAIL_TYPE
                        )
                );

        String previousThumbnailUrl = blog.getThumbnailUrl();

        String key = "thumbnails/" + blogId + "/" + UUID.randomUUID() + "." + imageType.getExtension();
        String thumbnailUrl = fileStorageService.upload(key, content, imageType.getContentType());

        blog.setThumbnailUrl(thumbnailUrl);

        try {
            blogRepository.save(blog);
        } catch (RuntimeException exception) {
            deleteQuietly(thumbnailUrl);
            throw exception;
        }

        deleteQuietly(previousThumbnailUrl);
        return blogMapper.toBlogResponse(blog);
    }

    private byte[] readContent(MultipartFile file) {

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void deleteQuietly(String url) {

        if (url == null) {
            return;
        }

        try {
            fileStorageService.delete(url);
        } catch (RuntimeException exception) {
            log.warn("Could not delete thumbnail object from storage", exception);
        }
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    public PageResponse<BlogSummaryResponse> getPublishedBlogs(
            Long categoryId, String keyword, String sort, int page, int size
    ) {

        Page<Blog> blogs = blogRepository.search(
                ContentStatus.PUBLISHED,
                categoryId,
                normalizeKeyword(keyword),
                pageRequest(page, size, sortFor(sort))
        );

        return toPageResponse(blogs);
    }

    @Transactional
    public BlogResponse getPublishedBlog(Long id) {

        Blog blog = requirePublishedBlog(id);

        blogRepository.incrementViewCount(id);

        BlogResponse response = blogMapper.toBlogResponse(blog);

        // The JPQL update bypasses the persistence context, so the entity we hold
        // still has the old number. Reflect the increment we just made.
        response.setViewCount(blog.getViewCount() + 1);

        return response;
    }

    @Transactional(readOnly = true)
    public List<BlogSummaryResponse> getRelatedBlogs(Long id) {

        Blog blog = requirePublishedBlog(id);

        return blogRepository
                .findByCategoryIdAndStatusAndIdNot(
                        blog.getCategory().getId(),
                        ContentStatus.PUBLISHED,
                        id,
                        PageRequest.of(0, RELATED_LIMIT, POPULAR_FIRST)
                )
                .stream()
                .map(blogMapper::toBlogSummaryResponse)
                .toList();
    }

    /** A blank keyword means "no filter", not "match the empty string". */
    private static String normalizeKeyword(String keyword) {

        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    // An unrecognised sort is a client typo, not a reason to fail the whole request
    private static Sort sortFor(String sort) {

        if (sort == null) {
            return RECENTLY_PUBLISHED_FIRST;
        }

        return switch (sort) {
            case "popular" -> POPULAR_FIRST;
            case "mostViewed" -> MOST_VIEWED_FIRST;
            default -> RECENTLY_PUBLISHED_FIRST;
        };
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

        // BR-07: the owner, or an admin acting on someone else's post — editing or
        // deleting it, e.g. to take down a violation (FR-10-04). Shared by update and delete.
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
