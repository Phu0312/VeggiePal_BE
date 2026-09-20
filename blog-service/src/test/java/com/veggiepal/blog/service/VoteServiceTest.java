package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.entity.ContentVote;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.repository.BlogRepository;
import com.veggiepal.blog.repository.ContentVoteRepository;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    static final Long VOTER_ID = 8L;
    static final Long AUTHOR_ID = 7L;

    @Mock
    ContentVoteRepository contentVoteRepository;

    @Mock
    BlogRepository blogRepository;

    @Mock
    BlogService blogService;

    @InjectMocks
    VoteService voteService;

    static Blog publishedBlog() {
        return Blog.builder().id(10L).authorId(AUTHOR_ID).voteScore(4).build();
    }

    static ContentVote existingVote(int value) {
        return ContentVote.builder()
                .id(1L).userId(VOTER_ID).targetType(TargetType.BLOG).targetId(10L).value(value)
                .build();
    }

    // All nine rows of the delta table in the spec, section 6.2.
    // "no vote" is written as 0 for both the previous and the new value.
    @ParameterizedTest(name = "{0} then {1} moves the score by {2}")
    @CsvSource({
            "0,  1,  1",
            "0, -1, -1",
            "1, -1, -2",
            "-1,  1,  2",
            "1,  1,  0",
            "-1, -1,  0",
            "1,  0, -1",
            "-1,  0,  1",
            "0,  0,  0"
    })
    void voteDelta_matchesTheSpecTable(int previous, int next, int expectedDelta) {
        assertThat(VoteService.voteDelta(previous == 0 ? null : previous, next == 0 ? null : next))
                .isEqualTo(expectedDelta);
    }

    @Test
    void vote_firstTime_savesVoteAndAddsOne() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.empty());
        when(contentVoteRepository.save(any(ContentVote.class))).thenAnswer(call -> call.getArgument(0));

        var response = voteService.vote(VOTER_ID, 10L, 1);

        verify(blogRepository).addVoteScore(10L, 1);
        assertThat(response.getMyVote()).isEqualTo(1);
        assertThat(response.getVoteScore()).isEqualTo(5);
    }

    @Test
    void vote_flippingUpvoteToDownvote_movesScoreByTwo() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.of(existingVote(1)));
        when(contentVoteRepository.save(any(ContentVote.class))).thenAnswer(call -> call.getArgument(0));

        var response = voteService.vote(VOTER_ID, 10L, -1);

        verify(blogRepository).addVoteScore(10L, -2);
        assertThat(response.getVoteScore()).isEqualTo(2);
    }

    @Test
    void vote_sameValueTwice_doesNotTouchTheScore() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.of(existingVote(1)));
        when(contentVoteRepository.save(any(ContentVote.class))).thenAnswer(call -> call.getArgument(0));

        voteService.vote(VOTER_ID, 10L, 1);

        verify(blogRepository, never()).addVoteScore(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void vote_zeroOrOtherValue_throwsInvalidVoteValue() {
        assertThatThrownBy(() -> voteService.vote(VOTER_ID, 10L, 0))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_VOTE_VALUE);

        assertThatThrownBy(() -> voteService.vote(VOTER_ID, 10L, 5))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_VOTE_VALUE);

        verify(contentVoteRepository, never()).save(any());
    }

    // UC-04: "vote on OTHER users' posts"
    @Test
    void vote_onOwnBlog_throwsCannotVoteOwnContent() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());

        assertThatThrownBy(() -> voteService.vote(AUTHOR_ID, 10L, 1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_VOTE_OWN_CONTENT);

        verify(contentVoteRepository, never()).save(any());
    }

    @Test
    void vote_onUnpublishedBlog_propagatesBlogNotExisted() {
        when(blogService.requirePublishedBlog(10L))
                .thenThrow(new AppException(ErrorCode.BLOG_NOT_EXISTED));

        assertThatThrownBy(() -> voteService.vote(VOTER_ID, 10L, 1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_EXISTED);
    }

    @Test
    void removeVote_existingUpvote_subtractsOne() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        ContentVote existing = existingVote(1);
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.of(existing));

        var response = voteService.removeVote(VOTER_ID, 10L);

        verify(contentVoteRepository).delete(existing);
        verify(blogRepository).addVoteScore(10L, -1);
        assertThat(response.getMyVote()).isNull();
        assertThat(response.getVoteScore()).isEqualTo(3);
    }

    // Deleting something that is not there already achieves the intended result
    @Test
    void removeVote_whenNoVoteExists_isANoOp() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.empty());

        var response = voteService.removeVote(VOTER_ID, 10L);

        verify(contentVoteRepository, never()).delete(any());
        verify(blogRepository, never()).addVoteScore(any(), org.mockito.ArgumentMatchers.anyInt());
        assertThat(response.getMyVote()).isNull();
    }

    @Test
    void getMyVotes_returnsOneEntryPerRequestedBlog() {
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetIdIn(
                VOTER_ID, TargetType.BLOG, List.of(10L, 11L)))
                .thenReturn(List.of(existingVote(1)));

        List<com.veggiepal.blog.dto.response.VoteResponse> votes =
                voteService.getMyVotes(VOTER_ID, List.of(10L, 11L));

        assertThat(votes).hasSize(2);
        assertThat(votes.getFirst().getMyVote()).isEqualTo(1);
        assertThat(votes.getLast().getMyVote()).isNull();
    }

    @Test
    void getMyVotes_emptyList_throwsBlogIdsRequired() {
        assertThatThrownBy(() -> voteService.getMyVotes(VOTER_ID, List.of()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_IDS_REQUIRED);
    }

    @Test
    void getMyVotes_tooManyIds_throwsInvalidRequest() {
        List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList();

        assertThatThrownBy(() -> voteService.getMyVotes(VOTER_ID, ids))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
