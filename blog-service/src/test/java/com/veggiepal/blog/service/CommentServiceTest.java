package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.veggiepal.blog.dto.request.CommentRequest;
import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.entity.Comment;
import com.veggiepal.blog.enums.CommentStatus;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.CommentMapper;
import com.veggiepal.blog.moderation.ModerationDecision;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.repository.CommentRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    static final Long AUTHOR_ID = 7L;
    static final Long STRANGER_ID = 8L;
    static final Long ADMIN_ID = 1L;

    @Mock
    CommentRepository commentRepository;

    @Mock
    BlogService blogService;

    @Mock
    ContentModerationService contentModerationService;

    @Spy
    CommentMapper commentMapper = Mappers.getMapper(CommentMapper.class);

    @InjectMocks
    CommentService commentService;

    static Comment comment(Long id, Comment parent, CommentStatus status) {
        return Comment.builder()
                .id(id).authorId(AUTHOR_ID)
                .targetType(TargetType.BLOG).targetId(10L)
                .parent(parent).content("ngon quá").status(status)
                .build();
    }

    static CommentRequest request(Long parentCommentId) {
        return CommentRequest.builder()
                .targetType(TargetType.BLOG).targetId(10L)
                .parentCommentId(parentCommentId).content("ngon quá")
                .build();
    }

    @Test
    void createComment_approved_becomesVisible() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());

        java.util.concurrent.atomic.AtomicReference<Comment> saved = new java.util.concurrent.atomic.AtomicReference<>();
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> {
            saved.set(call.getArgument(0));
            return call.getArgument(0);
        });

        CommentResponse response = commentService.createComment(AUTHOR_ID, request(null));

        assertThat(response.getContent()).isEqualTo("ngon quá");
        assertThat(response.isDeleted()).isFalse();
        assertThat(saved.get().getStatus()).isEqualTo(CommentStatus.VISIBLE);
    }

    @Test
    void createComment_rejected_isHiddenInsteadOfVisible() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(contentModerationService.moderate(anyString()))
                .thenReturn(new ModerationResult(ModerationDecision.REJECTED, "abuse"));

        java.util.concurrent.atomic.AtomicReference<Comment> saved = new java.util.concurrent.atomic.AtomicReference<>();
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> {
            saved.set(call.getArgument(0));
            return call.getArgument(0);
        });

        commentService.createComment(AUTHOR_ID, request(null));

        assertThat(saved.get().getStatus()).isEqualTo(CommentStatus.HIDDEN);
    }

    @Test
    void createComment_onMissingBlog_propagatesBlogNotExisted() {
        when(blogService.requirePublishedBlog(10L))
                .thenThrow(new AppException(ErrorCode.BLOG_NOT_EXISTED));

        assertThatThrownBy(() -> commentService.createComment(AUTHOR_ID, request(null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_EXISTED);

        verify(commentRepository, never()).save(any());
    }

    // The column already accepts VIDEO; the API does not, yet
    @Test
    void createComment_onVideo_throwsUnsupportedTargetType() {
        CommentRequest videoRequest = CommentRequest.builder()
                .targetType(TargetType.VIDEO).targetId(10L).content("ngon quá").build();

        assertThatThrownBy(() -> commentService.createComment(AUTHOR_ID, videoRequest))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_TARGET_TYPE);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createComment_replyToRootComment_isAllowed() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment(5L, null, CommentStatus.VISIBLE)));
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        CommentResponse response = commentService.createComment(AUTHOR_ID, request(5L));

        assertThat(response.getParentCommentId()).isEqualTo(5L);
    }

    @Test
    void createComment_replyToAReply_throwsTooDeep() {
        Comment root = comment(5L, null, CommentStatus.VISIBLE);
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(commentRepository.findById(6L)).thenReturn(Optional.of(comment(6L, root, CommentStatus.VISIBLE)));

        assertThatThrownBy(() -> commentService.createComment(AUTHOR_ID, request(6L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_REPLY_TOO_DEEP);
    }

    @Test
    void createComment_parentOnDifferentBlog_throwsInvalidParent() {
        Comment otherThread = comment(5L, null, CommentStatus.VISIBLE);
        otherThread.setTargetId(99L);

        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(commentRepository.findById(5L)).thenReturn(Optional.of(otherThread));

        assertThatThrownBy(() -> commentService.createComment(AUTHOR_ID, request(5L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COMMENT_PARENT);
    }

    @Test
    void updateComment_byStranger_throwsUnauthorized() {
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment(5L, null, CommentStatus.VISIBLE)));

        assertThatThrownBy(() -> commentService.updateComment(STRANGER_ID, 5L, request(null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    // A takedown is final: the author who was moderated away must not be able to
    // resurrect the same comment by editing it back to something innocuous.
    @Test
    void updateComment_deletedComment_throwsCommentNotExisted() {
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment(5L, null, CommentStatus.DELETED)));

        assertThatThrownBy(() -> commentService.updateComment(AUTHOR_ID, 5L, request(null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_EXISTED);

        verify(contentModerationService, never()).moderate(anyString());
        verify(commentRepository, never()).save(any());
    }

    @Test
    void updateComment_byOwner_editsAndReappliesModeration() {
        Comment existing = comment(5L, null, CommentStatus.VISIBLE);
        when(commentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        CommentRequest edit = CommentRequest.builder()
                .targetType(TargetType.BLOG).targetId(10L).content("ngon hơn nữa").build();

        CommentResponse response = commentService.updateComment(AUTHOR_ID, 5L, edit);

        assertThat(response.getContent()).isEqualTo("ngon hơn nữa");
        assertThat(existing.getContent()).isEqualTo("ngon hơn nữa");
        verify(commentRepository).save(existing);
    }

    // Soft delete: the row stays so its replies do not become orphans
    @Test
    void deleteComment_byOwner_marksDeletedAndKeepsTheRow() {
        Comment existing = comment(5L, null, CommentStatus.VISIBLE);
        when(commentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        commentService.deleteComment(AUTHOR_ID, false, 5L);

        assertThat(existing.getStatus()).isEqualTo(CommentStatus.DELETED);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_byAdmin_isAllowedOnSomeoneElsesComment() {
        Comment existing = comment(5L, null, CommentStatus.VISIBLE);
        when(commentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        commentService.deleteComment(ADMIN_ID, true, 5L);

        assertThat(existing.getStatus()).isEqualTo(CommentStatus.DELETED);
    }

    @Test
    void getRootComments_deletedComment_hidesContentButKeepsTheEntry() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());

        Comment deleted = comment(5L, null, CommentStatus.DELETED);
        // Spelled out rather than referencing PUBLICLY_VISIBLE: an expectation taken from
        // the field under test moves with it and can never fail. HIDDEN must stay out —
        // that is the status a moderator's rejection assigns.
        when(commentRepository.findByTargetTypeAndTargetIdAndParentIsNullAndStatusIn(
                any(), any(), eq(Set.of(CommentStatus.VISIBLE, CommentStatus.DELETED)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deleted)));
        when(commentRepository.countRepliesByParentIds(any(), any())).thenReturn(List.of());

        PageResponse<CommentResponse> result =
                commentService.getRootComments(TargetType.BLOG, 10L, 0, 20);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getContent()).isNull();
        assertThat(result.getItems().getFirst().isDeleted()).isTrue();
    }

    @Test
    void getRootComments_emptyPage_skipsTheReplyCountQuery() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(commentRepository.findByTargetTypeAndTargetIdAndParentIsNullAndStatusIn(
                any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<CommentResponse> result =
                commentService.getRootComments(TargetType.BLOG, 10L, 0, 20);

        assertThat(result.getItems()).isEmpty();
        verify(commentRepository, never()).countRepliesByParentIds(any(), any());
    }

    // Sibling of the already-fixed "author revives a deleted comment" bug: a blog
    // takedown, or a re-moderation to REJECTED, must close the thread to readers too.
    @Test
    void getRootComments_targetNotPublished_throwsBlogNotExisted() {
        when(blogService.requirePublishedBlog(10L))
                .thenThrow(new AppException(ErrorCode.BLOG_NOT_EXISTED));

        assertThatThrownBy(() -> commentService.getRootComments(TargetType.BLOG, 10L, 0, 20))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_EXISTED);

        verify(commentRepository, never())
                .findByTargetTypeAndTargetIdAndParentIsNullAndStatusIn(any(), any(), any(), any());
    }

    // A VIDEO target must answer UNSUPPORTED_TARGET_TYPE rather than have its id
    // looked up against blogs (requireSupportedTarget must run before the blog check).
    @Test
    void getRootComments_videoTarget_throwsUnsupportedTargetTypeWithoutLoadingBlog() {
        assertThatThrownBy(() -> commentService.getRootComments(TargetType.VIDEO, 10L, 0, 20))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_TARGET_TYPE);

        verify(blogService, never()).requirePublishedBlog(any());
    }

    // A reply cannot itself have replies, so its replyCount is always hardcoded to zero
    @Test
    void getReplies_returnsPubliclyVisibleRepliesWithZeroReplyCount() {
        Comment root = comment(5L, null, CommentStatus.VISIBLE);
        Comment reply = comment(6L, root, CommentStatus.VISIBLE);
        when(commentRepository.findById(5L)).thenReturn(Optional.of(root));
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(commentRepository.findByParentIdAndStatusIn(
                eq(5L), eq(Set.of(CommentStatus.VISIBLE, CommentStatus.DELETED)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reply)));

        PageResponse<CommentResponse> result = commentService.getReplies(5L, 0, 20);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getParentCommentId()).isEqualTo(5L);
        assertThat(result.getItems().getFirst().getReplyCount()).isEqualTo(0L);
        verify(commentRepository, never()).countRepliesByParentIds(any(), any());
    }

    // Same sibling-bug coverage as getRootComments, but derived from the parent
    // comment's target since getReplies is only given a comment id.
    @Test
    void getReplies_targetNotPublished_throwsBlogNotExisted() {
        Comment root = comment(5L, null, CommentStatus.VISIBLE);
        when(commentRepository.findById(5L)).thenReturn(Optional.of(root));
        when(blogService.requirePublishedBlog(10L))
                .thenThrow(new AppException(ErrorCode.BLOG_NOT_EXISTED));

        assertThatThrownBy(() -> commentService.getReplies(5L, 0, 20))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_EXISTED);

        verify(commentRepository, never()).findByParentIdAndStatusIn(any(), any(), any());
    }

    // findComment must still answer COMMENT_NOT_EXISTED for a comment id that
    // simply does not exist, rather than a confusing BLOG_NOT_EXISTED.
    @Test
    void getReplies_missingComment_throwsCommentNotExisted() {
        when(commentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.getReplies(5L, 0, 20))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_NOT_EXISTED);

        verify(blogService, never()).requirePublishedBlog(any());
    }
}
