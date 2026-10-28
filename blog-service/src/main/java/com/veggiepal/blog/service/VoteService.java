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
import com.veggiepal.blog.entity.Video;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.repository.BlogRepository;
import com.veggiepal.blog.repository.ContentVoteRepository;
import com.veggiepal.blog.repository.VideoRepository;

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
    VideoRepository videoRepository;
    VideoService videoService;

    /** What a vote needs to know about its target, whichever table it lives in. */
    private record Target(TargetType type, Long id, Long authorId, Integer voteScore) {
    }

    @Transactional
    public VoteResponse vote(Long userId, Long blogId, Integer value) {
        return vote(userId, TargetType.BLOG, blogId, value);
    }

    @Transactional
    public VoteResponse voteVideo(Long userId, Long videoId, Integer value) {
        return vote(userId, TargetType.VIDEO, videoId, value);
    }

    @Transactional
    public VoteResponse removeVote(Long userId, Long blogId) {
        return removeVote(userId, TargetType.BLOG, blogId);
    }

    @Transactional
    public VoteResponse removeVideoVote(Long userId, Long videoId) {
        return removeVote(userId, TargetType.VIDEO, videoId);
    }

    public List<VoteResponse> getMyVotes(Long userId, List<Long> blogIds) {

        if (blogIds == null || blogIds.isEmpty()) {
            throw new AppException(ErrorCode.BLOG_IDS_REQUIRED);
        }

        return getMyVotes(userId, TargetType.BLOG, blogIds);
    }

    public List<VoteResponse> getMyVideoVotes(Long userId, List<Long> videoIds) {

        if (videoIds == null || videoIds.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        return getMyVotes(userId, TargetType.VIDEO, videoIds);
    }

    private VoteResponse vote(Long userId, TargetType type, Long targetId, Integer value) {

        if (value == null || (value != 1 && value != -1)) {
            throw new AppException(ErrorCode.INVALID_VOTE_VALUE);
        }

        Target target = requireTarget(type, targetId);

        // UC-04: members vote on OTHER people's content
        if (target.authorId().equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_VOTE_OWN_CONTENT);
        }

        Optional<ContentVote> existing = findVote(userId, type, targetId);

        Integer previous = existing.map(ContentVote::getValue).orElse(null);

        // Task sheet US5: the same vote a second time takes it back, so a heart button can
        // send 1 on every click. The opposite value still switches the vote, keeping downvote.
        if (value.equals(previous)) {
            contentVoteRepository.delete(existing.get());
            return applyDelta(target, previous, null);
        }

        ContentVote vote = existing.orElseGet(() -> ContentVote.builder()
                .userId(userId)
                .targetType(type)
                .targetId(targetId)
                .build());

        vote.setValue(value);
        contentVoteRepository.save(vote);

        return applyDelta(target, previous, value);
    }

    private VoteResponse removeVote(Long userId, TargetType type, Long targetId) {

        Target target = requireTarget(type, targetId);

        Optional<ContentVote> existing = findVote(userId, type, targetId);

        if (existing.isEmpty()) {
            // Nothing to undo; the caller already has what they asked for
            return response(type, targetId, null, target.voteScore());
        }

        Integer previous = existing.get().getValue();
        contentVoteRepository.delete(existing.get());

        return applyDelta(target, previous, null);
    }

    private List<VoteResponse> getMyVotes(Long userId, TargetType type, List<Long> targetIds) {

        if (targetIds.size() > MAX_BLOG_IDS) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Map<Long, Integer> byTargetId = contentVoteRepository
                .findByUserIdAndTargetTypeAndTargetIdIn(userId, type, targetIds)
                .stream()
                .collect(Collectors.toMap(ContentVote::getTargetId, ContentVote::getValue));

        // One entry per requested id, so the client can index the result directly
        return targetIds.stream()
                .map(targetId -> response(type, targetId, byTargetId.get(targetId), null))
                .toList();
    }

    private Target requireTarget(TargetType type, Long targetId) {

        return switch (type) {
            case BLOG -> {
                Blog blog = blogService.requirePublishedBlog(targetId);
                yield new Target(type, blog.getId(), blog.getAuthorId(), blog.getVoteScore());
            }
            case VIDEO -> {
                Video video = videoService.requirePublishedVideo(targetId);
                yield new Target(type, video.getId(), video.getAuthorId(), video.getVoteScore());
            }
        };
    }

    private Optional<ContentVote> findVote(Long userId, TargetType type, Long targetId) {

        return contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId);
    }

    private VoteResponse applyDelta(Target target, Integer previous, Integer next) {

        int delta = voteDelta(previous, next);

        if (delta != 0) {
            switch (target.type()) {
                case BLOG -> blogRepository.addVoteScore(target.id(), delta);
                case VIDEO -> videoRepository.addVoteScore(target.id(), delta);
            }
        }

        // The JPQL update bypasses the persistence context, so add the delta here too
        return response(target.type(), target.id(), next, target.voteScore() + delta);
    }

    /**
     * The whole delta table in the spec collapses to "new minus old" once
     * "not voted" counts as zero.
     */
    static int voteDelta(Integer previous, Integer next) {

        return (next == null ? 0 : next) - (previous == null ? 0 : previous);
    }

    private static VoteResponse response(TargetType type, Long targetId, Integer myVote, Integer voteScore) {

        return VoteResponse.builder()
                .blogId(type == TargetType.BLOG ? targetId : null)
                .videoId(type == TargetType.VIDEO ? targetId : null)
                .myVote(myVote)
                .voteScore(voteScore)
                .build();
    }
}
