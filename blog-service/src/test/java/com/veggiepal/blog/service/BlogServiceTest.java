package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.entity.Category;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.BlogMapper;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.moderation.ModerationDecision;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.repository.BlogRepository;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    static final Long AUTHOR_ID = 7L;
    static final Long STRANGER_ID = 8L;
    static final Long ADMIN_ID = 1L;

    @Mock
    BlogRepository blogRepository;

    @Mock
    CategoryService categoryService;

    @Mock
    ContentModerationService contentModerationService;

    @Spy
    BlogMapper blogMapper = Mappers.getMapper(BlogMapper.class);

    @InjectMocks
    BlogService blogService;

    static Category category() {
        return Category.builder()
                .id(3L).name("Món chính").type(CategoryType.RECIPE_TYPE)
                .displayOrder((short) 0).active(true)
                .build();
    }

    static Blog blog(ContentStatus status) {
        return Blog.builder()
                .id(10L).authorId(AUTHOR_ID).category(category())
                .title("Đậu hũ sốt cà").content("x".repeat(50))
                .status(status).viewCount(0).voteScore(0)
                .build();
    }

    static BlogRequest request(boolean publish) {
        return BlogRequest.builder()
                .title("Đậu hũ sốt cà").content("x".repeat(50))
                .categoryId(3L).publish(publish)
                .build();
    }

    @Test
    void createBlog_publishFalse_savesDraftAndSkipsModeration() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.createBlog(AUTHOR_ID, request(false));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(response.getPublishedAt()).isNull();
        verify(contentModerationService, never()).moderate(anyString());
    }

    @Test
    void createBlog_publishTrueAndApproved_publishesAndStampsPublishedAt() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.createBlog(AUTHOR_ID, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
    }

    @Test
    void createBlog_publishTrueButRejected_returnsRejectedWithReason() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString()))
                .thenReturn(new ModerationResult(ModerationDecision.REJECTED, "spam"));
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.createBlog(AUTHOR_ID, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.REJECTED);
        assertThat(response.getModerationReason()).isEqualTo("spam");
        assertThat(response.getPublishedAt()).isNull();
    }

    @Test
    void createBlog_moderationPending_holdsContentBackFromPublic() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString()))
                .thenReturn(new ModerationResult(ModerationDecision.PENDING, "queued"));
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.createBlog(AUTHOR_ID, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PENDING);
        assertThat(response.getPublishedAt()).isNull();
    }

    @Test
    void createBlog_inactiveCategory_propagatesCategoryInactive() {
        when(categoryService.requireActiveCategory(3L))
                .thenThrow(new AppException(ErrorCode.CATEGORY_INACTIVE));

        assertThatThrownBy(() -> blogService.createBlog(AUTHOR_ID, request(false)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_INACTIVE);

        verify(blogRepository, never()).save(any());
    }

    // BR-02: editing something already public sends it back through moderation
    @Test
    void updateBlog_onPublished_reModerates() {
        Blog existing = blog(ContentStatus.PUBLISHED);
        existing.setPublishedAt(LocalDateTime.now().minusDays(3));

        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.updateBlog(AUTHOR_ID, false, 10L, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        verify(contentModerationService).moderate(anyString());
    }

    @Test
    void updateBlog_onDraft_staysDraftWithoutModeration() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.DRAFT)));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.updateBlog(AUTHOR_ID, false, 10L, request(false));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.DRAFT);
        verify(contentModerationService, never()).moderate(anyString());
    }

    // How a member fixes a rejected post and tries again
    @Test
    void updateBlog_onRejected_reModeratesAndCanBecomePublished() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.REJECTED)));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.updateBlog(AUTHOR_ID, false, 10L, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
    }

    @Test
    void updateBlog_republished_keepsOriginalPublishedAt() {
        LocalDateTime original = LocalDateTime.now().minusDays(3);
        Blog existing = blog(ContentStatus.PUBLISHED);
        existing.setPublishedAt(original);

        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        blogService.updateBlog(AUTHOR_ID, false, 10L, request(true));

        assertThat(existing.getPublishedAt()).isEqualTo(original);
    }

    @Test
    void updateBlog_byStranger_throwsUnauthorized() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.DRAFT)));

        assertThatThrownBy(() -> blogService.updateBlog(STRANGER_ID, false, 10L, request(false)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(blogRepository, never()).save(any());
    }

    // BR-07 / FR-10-04: an admin may take down someone else's post
    @Test
    void deleteBlog_byAdmin_deletesSomeoneElsesPost() {
        Blog existing = blog(ContentStatus.PUBLISHED);
        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));

        blogService.deleteBlog(ADMIN_ID, true, 10L);

        verify(blogRepository).delete(existing);
    }

    @Test
    void deleteBlog_byStranger_throwsUnauthorized() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.PUBLISHED)));

        assertThatThrownBy(() -> blogService.deleteBlog(STRANGER_ID, false, 10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(blogRepository, never()).delete(any());
    }

    @Test
    void submitBlog_fromDraft_runsModeration() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.DRAFT)));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.submitBlog(AUTHOR_ID, 10L);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
    }

    @Test
    void submitBlog_whenAlreadyPublished_throwsInvalidTransition() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.PUBLISHED)));

        assertThatThrownBy(() -> blogService.submitBlog(AUTHOR_ID, 10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_BLOG_STATUS_TRANSITION);
    }

    // A draft's category may have been deactivated after it was written; submit
    // must not let it slip past that guard just because create/update aren't in play.
    @Test
    void submitBlog_categoryDeactivatedSinceDraft_throwsCategoryInactive() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.DRAFT)));
        when(categoryService.requireActiveCategory(3L))
                .thenThrow(new AppException(ErrorCode.CATEGORY_INACTIVE));

        assertThatThrownBy(() -> blogService.submitBlog(AUTHOR_ID, 10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_INACTIVE);

        verify(contentModerationService, never()).moderate(anyString());
        verify(blogRepository, never()).save(any());
    }

    @Test
    void requirePublishedBlog_draft_looksExactlyLikeMissing() {
        when(blogRepository.findByIdAndStatus(10L, ContentStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogService.requirePublishedBlog(10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_EXISTED);
    }
}
