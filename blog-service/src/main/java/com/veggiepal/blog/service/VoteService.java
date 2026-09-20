package com.veggiepal.blog.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.blog.dto.response.VoteResponse;
import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.entity.ContentVote;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.repository.BlogRepository;
import com.veggiepal.blog.repository.ContentVoteRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoteService {

    static final int MAX_BLOG_IDS = 100;

    ContentVoteRepository contentVoteRepository;
    BlogRepository blogRepository;
    BlogService blogService;

    @Transactional
    public VoteResponse vote(Long userId, Long blogId, Integer value) {

        if (value == null || (value != 1 && value != -1)) {
            throw new AppException(ErrorCode.INVALID_VOTE_VALUE);
        }

        Blog blog = requireVotableBlog(userId, blogId);

        Optional<ContentVote> existing = findVote(userId, blogId);

        Integer previous = existing.map(ContentVote::getValue).orElse(null);

        ContentVote vote = existing.orElseGet(() -> ContentVote.builder()
                .userId(userId)
                .targetType(TargetType.BLOG)
                .targetId(blogId)
                .build());

        vote.setValue(value);
        contentVoteRepository.save(vote);

        return applyDelta(blog, previous, value);
    }

    @Transactional
    public VoteResponse removeVote(Long userId, Long blogId) {

        Blog blog = blogService.requirePublishedBlog(blogId);

        Optional<ContentVote> existing = findVote(userId, blogId);

        if (existing.isEmpty()) {
            // Nothing to undo; the caller already has what they asked for
            return response(blogId, null, blog.getVoteScore());
        }

        Integer previous = existing.get().getValue();
        contentVoteRepository.delete(existing.get());

        return applyDelta(blog, previous, null);
    }

    public List<VoteResponse> getMyVotes(Long userId, List<Long> blogIds) {

        if (blogIds == null || blogIds.isEmpty()) {
            throw new AppException(ErrorCode.BLOG_IDS_REQUIRED);
        }

        if (blogIds.size() > MAX_BLOG_IDS) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Map<Long, Integer> byBlogId = contentVoteRepository
                .findByUserIdAndTargetTypeAndTargetIdIn(userId, TargetType.BLOG, blogIds)
                .stream()
                .collect(Collectors.toMap(ContentVote::getTargetId, ContentVote::getValue));

        // One entry per requested id, so the client can index the result directly
        return blogIds.stream()
                .map(blogId -> response(blogId, byBlogId.get(blogId), null))
                .toList();
    }

    private Blog requireVotableBlog(Long userId, Long blogId) {

        Blog blog = blogService.requirePublishedBlog(blogId);

        // UC-04: members vote on OTHER people's posts
        if (blog.getAuthorId().equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_VOTE_OWN_CONTENT);
        }

        return blog;
    }

    private Optional<ContentVote> findVote(Long userId, Long blogId) {

        return contentVoteRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, TargetType.BLOG, blogId);
    }

    private VoteResponse applyDelta(Blog blog, Integer previous, Integer next) {

        int delta = voteDelta(previous, next);

        if (delta != 0) {
            blogRepository.addVoteScore(blog.getId(), delta);
        }

        // The JPQL update bypasses the persistence context, so add the delta here too
        return response(blog.getId(), next, blog.getVoteScore() + delta);
    }

    /**
     * The whole delta table in the spec collapses to "new minus old" once
     * "not voted" counts as zero.
     */
    static int voteDelta(Integer previous, Integer next) {

        return (next == null ? 0 : next) - (previous == null ? 0 : previous);
    }

    private static VoteResponse response(Long blogId, Integer myVote, Integer voteScore) {

        return VoteResponse.builder()
                .blogId(blogId)
                .myVote(myVote)
                .voteScore(voteScore)
                .build();
    }
}
