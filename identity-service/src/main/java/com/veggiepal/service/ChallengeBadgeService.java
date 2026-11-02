package com.veggiepal.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.veggiepal.dto.response.BadgeResponse;
import com.veggiepal.dto.response.ChallengeProgressResponse;
import com.veggiepal.entity.ChallengeCheckIn;
import com.veggiepal.entity.SevenDayChallenge;
import com.veggiepal.entity.UserBadge;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.repository.ChallengeCheckInRepository;
import com.veggiepal.repository.SevenDayChallengeRepository;
import com.veggiepal.repository.UserBadgeRepository;
import com.veggiepal.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChallengeBadgeService {
    UserRepository users;
    SevenDayChallengeRepository challenges;
    ChallengeCheckInRepository checkIns;
    UserBadgeRepository badges;
    Clock clock;

    @Transactional
    public ChallengeProgressResponse join(Long userId) {
        lockUser(userId);
        LocalDate today = LocalDate.now(clock);
        challenges.findFirstByUserIdOrderByIdDesc(userId).ifPresent(previous -> {
            refreshStatus(previous, today);
            if ("IN_PROGRESS".equals(previous.getStatus())) {
                throw new AppException(ErrorCode.CHALLENGE_ALREADY_ACTIVE);
            }
        });
        SevenDayChallenge challenge = challenges.save(new SevenDayChallenge(userId, today));
        return view(challenge, 0, today);
    }

    @Transactional
    public ChallengeProgressResponse checkIn(Long userId) {
        lockUser(userId);
        SevenDayChallenge challenge = challenges.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CHALLENGE_NOT_JOINED));
        LocalDate today = LocalDate.now(clock);
        long completed = checkIns.countByChallengeId(challenge.getId());
        refreshStatus(challenge, today, completed);
        if ("FAILED".equals(challenge.getStatus())) return view(challenge, completed, today);
        if (!"IN_PROGRESS".equals(challenge.getStatus())) {
            throw new AppException(ErrorCode.CHALLENGE_NOT_ACTIVE);
        }
        int day = (int) ChronoUnit.DAYS.between(challenge.getStartedOn(), today) + 1;
        if (checkIns.existsByChallengeIdAndDayNumber(challenge.getId(), day)) {
            throw new AppException(ErrorCode.CHALLENGE_ALREADY_CHECKED);
        }
        checkIns.save(new ChallengeCheckIn(challenge.getId(), day, today));
        completed++;
        if (day == 7) {
            challenge.setStatus("COMPLETED");
            challenges.save(challenge);
            grantIfAbsent(userId, "ACHIEVEMENT", null);
        }
        return view(challenge, completed, today);
    }

    @Transactional
    public ChallengeProgressResponse progress(Long userId) {
        SevenDayChallenge challenge = challenges.findFirstByUserIdOrderByIdDesc(userId)
                .orElseThrow(() -> new AppException(ErrorCode.CHALLENGE_NOT_JOINED));
        LocalDate today = LocalDate.now(clock);
        long completed = checkIns.countByChallengeId(challenge.getId());
        refreshStatus(challenge, today, completed);
        return view(challenge, completed, today);
    }

    @Transactional(readOnly = true)
    public List<BadgeResponse> activeBadges(Long userId) {
        return badges.findByUserIdAndRevokedAtIsNullOrderByGrantedAtDesc(userId).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BadgeResponse> badgeHistory(Long userId) {
        requireUser(userId);
        return badges.findByUserIdOrderByGrantedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public BadgeResponse grantVerification(Long adminId, Long userId) {
        lockUser(userId);
        return toResponse(grantIfAbsent(userId, "VERIFIED", adminId));
    }

    @Transactional
    public BadgeResponse revoke(Long adminId, Long userId, Long badgeId, String reason) {
        String cleanReason = reason == null ? "" : reason.trim();
        if (cleanReason.isEmpty() || cleanReason.length() > 500) {
            throw new AppException(ErrorCode.BADGE_REVOCATION_REASON_REQUIRED);
        }
        lockUser(userId);
        UserBadge badge = badges.findByIdAndUserId(badgeId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.BADGE_NOT_FOUND));
        if (badge.getRevokedAt() != null) throw new AppException(ErrorCode.BADGE_ALREADY_REVOKED);
        badge.setRevokedAt(LocalDateTime.now(clock));
        badge.setRevokedBy(adminId);
        badge.setRevocationReason(cleanReason);
        badges.save(badge);
        return toResponse(badge);
    }

    private UserBadge grantIfAbsent(Long userId, String type, Long grantedBy) {
        return badges.findFirstByUserIdAndBadgeTypeAndRevokedAtIsNull(userId, type)
                .orElseGet(() -> {
                    UserBadge badge = new UserBadge(userId, type, LocalDateTime.now(clock), grantedBy);
                    badges.save(badge);
                    return badge;
                });
    }

    private void refreshStatus(SevenDayChallenge challenge, LocalDate today) {
        refreshStatus(challenge, today, checkIns.countByChallengeId(challenge.getId()));
    }

    private void refreshStatus(SevenDayChallenge challenge, LocalDate today, long completed) {
        if (!"IN_PROGRESS".equals(challenge.getStatus())) return;
        long daysElapsed = ChronoUnit.DAYS.between(challenge.getStartedOn(), today);
        if (daysElapsed > 6 || completed < daysElapsed) {
            challenge.setStatus("FAILED");
            challenges.save(challenge);
        }
    }

    private ChallengeProgressResponse view(SevenDayChallenge challenge, long completed, LocalDate today) {
        int day = (int) ChronoUnit.DAYS.between(challenge.getStartedOn(), today) + 1;
        boolean checkedToday = day >= 1 && day <= 7
                && checkIns.existsByChallengeIdAndDayNumber(challenge.getId(), day);
        return new ChallengeProgressResponse(challenge.getId(), challenge.getStartedOn(), (int) completed,
                7, (int) completed * 10, challenge.getStatus(), checkedToday);
    }

    private BadgeResponse toResponse(UserBadge badge) {
        return new BadgeResponse(badge.getId(), badge.getBadgeType(), badge.getGrantedAt(),
                badge.getRevokedAt(), badge.getRevocationReason());
    }

    private void lockUser(Long userId) {
        users.lockById(userId).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));
    }

    private void requireUser(Long userId) {
        if (!users.existsById(userId)) throw new AppException(ErrorCode.USER_NOT_EXISTED);
    }
}
