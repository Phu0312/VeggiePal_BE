package com.veggiepal.blog.service;

import com.veggiepal.blog.dto.response.*;
import com.veggiepal.blog.entity.*;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.BlogMapper;
import com.veggiepal.blog.mapper.VideoMapper;
import com.veggiepal.blog.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminContentService {
    private final BlogRepository blogs;
    private final VideoRepository videos;
    private final ContentStatusAuditRepository audits;
    private final BlogMapper blogMapper;
    private final VideoMapper videoMapper;

    @Transactional(readOnly = true)
    public PageResponse<BlogSummaryResponse> blogs(ContentStatus status, Long authorId, String keyword,
                                                     int page, int size) {
        Page<Blog> result = blogs.adminSearch(status, authorId, term(keyword), paging(page, size));
        return response(result, result.getContent().stream().map(blogMapper::toOwnBlogSummaryResponse).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<VideoResponse> videos(ContentStatus status, Long authorId, String keyword,
                                               int page, int size) {
        Page<Video> result = videos.adminSearch(status, authorId, term(keyword), paging(page, size));
        return response(result, result.getContent().stream().map(videoMapper::toVideoResponse).toList());
    }

    @Transactional(readOnly = true)
    public VideoResponse video(Long id) {
        return videoMapper.toVideoResponse(videos.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_EXISTED)));
    }

    @Transactional
    public BlogResponse changeBlogStatus(Long id, Long adminId, ContentStatus target, String reason) {
        Blog blog = blogs.findForModeration(id)
                .orElseThrow(() -> new AppException(ErrorCode.BLOG_NOT_EXISTED));
        if (!allowed(blog.getStatus(), target))
            throw new AppException(ErrorCode.INVALID_BLOG_STATUS_TRANSITION);
        String explanation = requireReason(reason);
        ContentStatus before = blog.getStatus();
        blog.setStatus(target);
        blog.setModerationRequestedAt(null);
        blog.setModerationReason(target == ContentStatus.REJECTED ? explanation : null);
        if (target == ContentStatus.PUBLISHED && blog.getPublishedAt() == null)
            blog.setPublishedAt(LocalDateTime.now());
        audits.save(new ContentStatusAudit("BLOG", id, before, target, explanation, adminId));
        return blogMapper.toBlogResponse(blog);
    }

    @Transactional
    public VideoResponse changeVideoStatus(Long id, Long adminId, ContentStatus target, String reason) {
        Video video = videos.findForModeration(id)
                .orElseThrow(() -> new AppException(ErrorCode.VIDEO_NOT_EXISTED));
        if (!allowed(video.getStatus(), target))
            throw new AppException(ErrorCode.INVALID_VIDEO_STATUS_TRANSITION);
        String explanation = requireReason(reason);
        ContentStatus before = video.getStatus();
        video.setStatus(target);
        video.setModerationReason(target == ContentStatus.REJECTED ? explanation : null);
        if (target == ContentStatus.PUBLISHED && video.getPublishedAt() == null)
            video.setPublishedAt(LocalDateTime.now());
        audits.save(new ContentStatusAudit("VIDEO", id, before, target, explanation, adminId));
        return videoMapper.toVideoResponse(video);
    }

    private static boolean allowed(ContentStatus before, ContentStatus target) {
        return (before == ContentStatus.PENDING || before == ContentStatus.REJECTED)
                && (target == ContentStatus.PUBLISHED || target == ContentStatus.REJECTED);
    }

    private static String requireReason(String reason) {
        if (reason == null || reason.isBlank() || reason.length() > 1000)
            throw new AppException(ErrorCode.INVALID_REQUEST);
        return reason.trim();
    }

    private static String term(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static PageRequest paging(int page, int size) {
        int safeSize = Math.clamp(size, 1, 100);
        return PageRequest.of(Math.min(Math.max(page, 0), Integer.MAX_VALUE / safeSize), safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private static <T> PageResponse<T> response(Page<?> page, List<T> items) {
        return PageResponse.<T>builder().items(items).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages()).build();
    }
}
