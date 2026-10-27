package com.veggiepal.service;

import com.veggiepal.entity.*;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.repository.*;
import com.veggiepal.dto.response.*;
import com.veggiepal.exception.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Service @RequiredArgsConstructor
public class FollowService {
    private final UserRepository users;
    private final UserFollowRepository follows;

    @Transactional public FollowStats follow(Long actor,Long target) {
        if(actor.equals(target)) throw new AppException(ErrorCode.CANNOT_FOLLOW_SELF);
        // Serializes mutations by the same actor, including concurrent retries and unfollow.
        active(users.lockById(actor).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
        requireActive(target);
        if(!follows.existsByFollowerIdAndFollowingId(actor,target)) follows.saveAndFlush(new UserFollow(actor,target));
        return statsUnchecked(actor,target);
    }
    @Transactional public FollowStats unfollow(Long actor,Long target) {
        if(actor.equals(target)) throw new AppException(ErrorCode.CANNOT_FOLLOW_SELF);
        active(users.lockById(actor).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED)));
        follows.deleteByFollowerIdAndFollowingId(actor,target);
        return statsUnchecked(actor,target);
    }
    @Transactional(readOnly=true) public FollowStats stats(Long actor,Long target) {
        requireActive(actor); requireActive(target); return statsUnchecked(actor,target);
    }
    private FollowStats statsUnchecked(Long actor,Long target) {
        boolean visible=users.findById(target).map(u -> u.getStatus()==UserStatus.ACTIVE).orElse(false);
        return new FollowStats(visible?follows.followerCount(target,UserStatus.ACTIVE):0,
                visible?follows.followingCount(target,UserStatus.ACTIVE):0,
                visible && follows.existsByFollowerIdAndFollowingId(actor,target));
    }
    @Transactional(readOnly=true) public PageResponse<PublicUserResponse> list(Long actor,Long target,boolean following,int page,int size) {
        requireActive(actor);requireActive(target);
        int safeSize=Math.clamp(size,1,100);int safePage=Math.min(Math.max(0,page),Integer.MAX_VALUE/safeSize);
        var pageable=PageRequest.of(safePage,safeSize,Sort.by("id"));
        Page<User> result=following?follows.following(target,UserStatus.ACTIVE,pageable):follows.followers(target,UserStatus.ACTIVE,pageable);
        return new PageResponse<>(result.getContent().stream().map(u -> new PublicUserResponse(u.getId(),u.getFullName(),u.getAvatarUrl())).toList(),
                result.getNumber(),result.getSize(),result.getTotalElements(),result.getTotalPages());
    }
    @Transactional(readOnly=true) public List<Long> followingIds(Long actor) {
        requireActive(actor);return follows.followingIds(actor,UserStatus.ACTIVE);
    }
    private User requireActive(Long id) { return active(users.findById(id).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED))); }
    private User active(User user) {
        if(user.getStatus()!=UserStatus.ACTIVE) throw new AppException(ErrorCode.USER_NOT_EXISTED);
        return user;
    }
}
