package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.dto.response.PageResponse;
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

    @Mock
    FileStorageService fileStorageService;

    @Spy
    BlogMapper blogMapper = Mappers.getMapper(BlogMapper.class);

    @InjectMocks
    BlogService blogService;

    static byte[] pngBytes() {
        byte[] content = new byte[32];
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, content, 0, signature.length);
        return content;
    }

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
    // Task sheet US3/US6: an admin takedown hides the post but keeps it, so the owner still
    // sees it in their dashboard with a "Bị cấm" badge. A hard delete would erase it.
    @Test
    void deleteBlog_byAdminOnSomeoneElsesPost_bansItInsteadOfDeleting() {
        Blog existing = blog(ContentStatus.PUBLISHED);
        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));

        blogService.deleteBlog(ADMIN_ID, true, 10L);

        assertThat(existing.getStatus()).isEqualTo(ContentStatus.BANNED);
        verify(blogRepository).save(existing);
        verify(blogRepository, never()).delete(any());
    }

    @Test
    void deleteBlog_byOwner_deletesIt() {
        Blog existing = blog(ContentStatus.PUBLISHED);
        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));

        blogService.deleteBlog(AUTHOR_ID, false, 10L);

        verify(blogRepository).delete(existing);
    }

    // Banning is for other people's content. An admin removing their own post is just an
    // owner deleting it.
    @Test
    void deleteBlog_byAdminOnTheirOwnPost_deletesIt() {
        Blog existing = blog(ContentStatus.PUBLISHED);
        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));

        blogService.deleteBlog(AUTHOR_ID, true, 10L);

        verify(blogRepository).delete(existing);
    }

    // Editing re-runs moderation, which approves — so without this guard an owner could lift
    // an admin's ban just by saving the post again.
    @Test
    void updateBlog_onBanned_throwsInvalidTransitionWithoutModerating() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.BANNED)));

        assertThatThrownBy(() -> blogService.updateBlog(AUTHOR_ID, false, 10L, request(true)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_BLOG_STATUS_TRANSITION);

        verify(contentModerationService, never()).moderate(anyString());
        verify(blogRepository, never()).save(any());
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

    @Test
    void getPublishedBlogs_blankKeyword_isPassedAsNull() {
        when(blogRepository.search(eq(ContentStatus.PUBLISHED), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog(ContentStatus.PUBLISHED))));

        PageResponse<BlogSummaryResponse> result =
                blogService.getPublishedBlogs(null, "   ", null, 0, 20);

        assertThat(result.getItems()).hasSize(1);
        verify(blogRepository).search(eq(ContentStatus.PUBLISHED), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getPublishedBlogs_noMatch_returnsEmptyPageInsteadOfThrowing() {
        when(blogRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<BlogSummaryResponse> result =
                blogService.getPublishedBlogs(null, "không có gì", null, 0, 20);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getPublishedBlogs_sortPopular_ordersByVoteScore() {
        when(blogRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        blogService.getPublishedBlogs(null, null, "popular", 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).search(any(), any(), any(), pageable.capture());

        assertThat(pageable.getValue().getSort().getOrderFor("voteScore")).isNotNull();
    }

    @Test
    void getPublishedBlogs_unknownSort_fallsBackToNewest() {
        when(blogRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        blogService.getPublishedBlogs(null, null, "chaos", 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).search(any(), any(), any(), pageable.capture());

        assertThat(pageable.getValue().getSort().getOrderFor("publishedAt")).isNotNull();
    }

    @Test
    void getPublishedBlogs_oversizedPage_isClampedToHundred() {
        when(blogRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        blogService.getPublishedBlogs(null, null, null, -5, 5000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).search(any(), any(), any(), pageable.capture());

        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getPageNumber()).isZero();
    }

    @Test
    void getPublishedBlog_incrementsViewAndReportsTheNewCount() {
        Blog published = blog(ContentStatus.PUBLISHED);
        published.setViewCount(41);
        when(blogRepository.findByIdAndStatus(10L, ContentStatus.PUBLISHED))
                .thenReturn(Optional.of(published));

        BlogResponse response = blogService.getPublishedBlog(10L);

        verify(blogRepository).incrementViewCount(10L);
        // The JPQL update does not refresh the loaded entity, so the service adds the 1 itself
        assertThat(response.getViewCount()).isEqualTo(42);
    }

    @Test
    void getRelatedBlogs_excludesItselfAndCapsAtFive() {
        Blog published = blog(ContentStatus.PUBLISHED);
        when(blogRepository.findByIdAndStatus(10L, ContentStatus.PUBLISHED))
                .thenReturn(Optional.of(published));
        when(blogRepository.findByCategoryIdAndStatusAndIdNot(
                eq(3L), eq(ContentStatus.PUBLISHED), eq(10L), any(Pageable.class)))
                .thenReturn(List.of(blog(ContentStatus.PUBLISHED)));

        List<BlogSummaryResponse> related = blogService.getRelatedBlogs(10L);

        assertThat(related).hasSize(1);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).findByCategoryIdAndStatusAndIdNot(any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void uploadThumbnail_validPng_storesAndReplacesPreviousObject() {
        Blog existing = blog(ContentStatus.PUBLISHED);
        existing.setThumbnailUrl("http://localhost:9000/veggiepal-blog-thumbnails/old.png");

        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(fileStorageService.upload(anyString(), any(byte[].class), eq("image/png")))
                .thenReturn("http://localhost:9000/veggiepal-blog-thumbnails/new.png");
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        MockMultipartFile file =
                new MockMultipartFile("file", "cover.png", "image/png", pngBytes());

        BlogResponse response = blogService.uploadThumbnail(AUTHOR_ID, false, 10L, file);

        assertThat(response.getThumbnailUrl()).endsWith("new.png");

        InOrder inOrder = inOrder(fileStorageService, blogRepository);
        inOrder.verify(fileStorageService).upload(anyString(), any(byte[].class), anyString());
        inOrder.verify(blogRepository).save(any(Blog.class));
        // The previous object is only removed once the new URL is safely persisted
        inOrder.verify(fileStorageService).delete("http://localhost:9000/veggiepal-blog-thumbnails/old.png");
    }

    @Test
    void uploadThumbnail_emptyFile_throwsThumbnailRequired() {
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> blogService.uploadThumbnail(AUTHOR_ID, false, 10L, file))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.THUMBNAIL_REQUIRED);
    }

    // Between MAX_THUMBNAIL_BYTES (5MB) and the parser's own multipart limit (6MB):
    // the service's own size check must reject it before touching storage or the DB.
    @Test
    void uploadThumbnail_overMaxBytes_throwsThumbnailTooLarge() {
        byte[] oversized = new byte[(int) BlogService.MAX_THUMBNAIL_BYTES + 1];
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", oversized);

        assertThatThrownBy(() -> blogService.uploadThumbnail(AUTHOR_ID, false, 10L, file))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.THUMBNAIL_TOO_LARGE);

        verify(blogRepository, never()).findById(any());
        verify(fileStorageService, never()).upload(anyString(), any(byte[].class), anyString());
        verify(blogRepository, never()).save(any());
    }

    // Declared content type lying about the real bytes is the attack this blocks
    @Test
    void uploadThumbnail_contentTypeDoesNotMatchMagicBytes_throwsInvalidType() {
        MockMultipartFile file =
                new MockMultipartFile("file", "x.jpg", "image/jpeg", pngBytes());

        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.PUBLISHED)));

        assertThatThrownBy(() -> blogService.uploadThumbnail(AUTHOR_ID, false, 10L, file))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_THUMBNAIL_TYPE);

        verify(fileStorageService, never()).upload(anyString(), any(byte[].class), anyString());
        verify(blogRepository, never()).save(any());
    }

    @Test
    void uploadThumbnail_byStranger_throwsUnauthorized() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.PUBLISHED)));

        MockMultipartFile file =
                new MockMultipartFile("file", "cover.png", "image/png", pngBytes());

        assertThatThrownBy(() -> blogService.uploadThumbnail(STRANGER_ID, false, 10L, file))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(fileStorageService, never()).upload(anyString(), any(byte[].class), anyString());
        verify(blogRepository, never()).save(any());
    }
}
