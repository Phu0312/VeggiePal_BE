package com.veggiepal.blog.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.blog.dto.request.CommentRequest;
import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.entity.Comment;
import com.veggiepal.blog.enums.CommentStatus;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.CommentMapper;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.repository.CommentRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {

    // DELETED stays visible as a tombstone so a reply thread does not break apart
    static final Set<CommentStatus> PUBLICLY_VISIBLE =
            Set.of(CommentStatus.VISIBLE, CommentStatus.DELETED);

    static final Sort OLDEST_FIRST = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    CommentRepository commentRepository;
    BlogService blogService;
    ContentModerationService contentModerationService;
    CommentMapper commentMapper;

    @Transactional
    public CommentResponse createComment(Long authorId, CommentRequest request) {

        requireSupportedTarget(request.getTargetType());

        // Only published content can be commented on
        blogService.requirePublishedBlog(request.getTargetId());

        Comment parent = resolveParent(request);

        Comment comment = Comment.builder()
                .authorId(authorId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .parent(parent)
                .content(request.getContent())
                .status(CommentStatus.PENDING)
                .build();

        applyModeration(comment);

        commentRepository.save(comment);
        return toResponse(comment, 0L);
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentRequest request) {

        Comment comment = findComment(commentId);

        // Editing someone else's words is never an admin action
        if (!comment.getAuthorId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        comment.setContent(request.getContent());
        applyModeration(comment);

        commentRepository.save(comment);
        return toResponse(comment, null);
    }

    @Transactional
    public void deleteComment(Long userId, boolean admin, Long commentId) {

        Comment comment = findComment(commentId);

        // BR-07 plus FR-10-04: the owner, or an admin removing a violation
        if (!admin && !comment.getAuthorId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    public PageResponse<CommentResponse> getRootComments(
            TargetType targetType, Long targetId, int page, int size
    ) {

        Page<Comment> comments = commentRepository
                .findByTargetTypeAndTargetIdAndParentIsNullAndStatusIn(
                        targetType, targetId, PUBLICLY_VISIBLE,
                        BlogService.pageRequest(page, size, OLDEST_FIRST)
                );

        Map<Long, Long> replyCounts = replyCountsFor(comments.getContent());

        return toPageResponse(comments, comment -> replyCounts.getOrDefault(comment.getId(), 0L));
    }

    public PageResponse<CommentResponse> getReplies(Long commentId, int page, int size) {

        Page<Comment> replies = commentRepository.findByParentIdAndStatusIn(
                commentId, PUBLICLY_VISIBLE,
                BlogService.pageRequest(page, size, OLDEST_FIRST)
        );

        // A reply cannot have replies of its own, so the count is always zero
        return toPageResponse(replies, comment -> 0L);
    }

    private Map<Long, Long> replyCountsFor(List<Comment> roots) {

        if (roots.isEmpty()) {
            return Map.of();
        }

        return commentRepository
                .countRepliesByParentIds(roots.stream().map(Comment::getId).toList(), PUBLICLY_VISIBLE)
                .stream()
                .collect(Collectors.toMap(
                        CommentRepository.ReplyCount::getParentId,
                        CommentRepository.ReplyCount::getTotal
                ));
    }

    private Comment resolveParent(CommentRequest request) {

        if (request.getParentCommentId() == null) {
            return null;
        }

        Comment parent = findComment(request.getParentCommentId());

        // One level of replies only, as the ERD describes
        if (parent.getParent() != null) {
            throw new AppException(ErrorCode.COMMENT_REPLY_TOO_DEEP);
        }

        boolean sameThread = parent.getTargetType() == request.getTargetType()
                && parent.getTargetId().equals(request.getTargetId());

        if (!sameThread) {
            throw new AppException(ErrorCode.INVALID_COMMENT_PARENT);
        }

        return parent;
    }

    private void applyModeration(Comment comment) {

        ModerationResult result = contentModerationService.moderate(comment.getContent());

        comment.setStatus(switch (result.decision()) {
            case APPROVED -> CommentStatus.VISIBLE;
            case REJECTED -> CommentStatus.HIDDEN;
            case PENDING -> CommentStatus.PENDING;
        });
    }

    private static void requireSupportedTarget(TargetType targetType) {

        // The column already accepts VIDEO so no ALTER TABLE is needed later,
        // but there is no video API to point at yet.
        if (targetType != TargetType.BLOG) {
            throw new AppException(ErrorCode.UNSUPPORTED_TARGET_TYPE);
        }
    }

    private Comment findComment(Long commentId) {

        return commentRepository
                .findById(commentId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.COMMENT_NOT_EXISTED
                        )
                );
    }

    private PageResponse<CommentResponse> toPageResponse(
            Page<Comment> comments, Function<Comment, Long> replyCount
    ) {

        return PageResponse.<CommentResponse>builder()
                .items(comments.getContent().stream()
                        .map(comment -> toResponse(comment, replyCount.apply(comment)))
                        .toList())
                .page(comments.getNumber())
                .size(comments.getSize())
                .totalElements(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .build();
    }

    private CommentResponse toResponse(Comment comment, Long replyCount) {

        CommentResponse response = commentMapper.toCommentResponse(comment);

        boolean deleted = comment.getStatus() == CommentStatus.DELETED;

        response.setDeleted(deleted);
        response.setContent(deleted ? null : comment.getContent());
        response.setReplyCount(replyCount);

        return response;
    }
}
