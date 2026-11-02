package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import com.veggiepal.entity.SevenDayChallenge;
import com.veggiepal.entity.User;
import com.veggiepal.entity.UserBadge;
import com.veggiepal.exception.AppException;
import com.veggiepal.repository.ChallengeCheckInRepository;
import com.veggiepal.repository.SevenDayChallengeRepository;
import com.veggiepal.repository.UserBadgeRepository;
import com.veggiepal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChallengeBadgeServiceTest {
    @Mock UserRepository users;
    @Mock SevenDayChallengeRepository challenges;
    @Mock ChallengeCheckInRepository checkIns;
    @Mock UserBadgeRepository badges;

    private ChallengeBadgeService service(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Ho_Chi_Minh"));
        return new ChallengeBadgeService(users, challenges, checkIns, badges, clock);
    }

    @Test
    void joiningTwiceWhileActiveIsRejected() {
        SevenDayChallenge challenge = new SevenDayChallenge(7L, LocalDate.of(2026, 11, 2));
        when(users.lockById(7L)).thenReturn(Optional.of(new User()));
        when(challenges.findFirstByUserIdOrderByIdDesc(7L)).thenReturn(Optional.of(challenge));

        assertThatThrownBy(() -> service("2026-11-02T05:00:00Z").join(7L))
                .isInstanceOf(AppException.class);
    }

    @Test
    void checkInUsesVietnamCalendarDayAndAddsTenPoints() {
        SevenDayChallenge challenge = new SevenDayChallenge(7L, LocalDate.of(2026, 11, 2));
        when(users.lockById(7L)).thenReturn(Optional.of(new User()));
        when(challenges.findFirstByUserIdOrderByIdDesc(7L)).thenReturn(Optional.of(challenge));
        when(checkIns.countByChallengeId(challenge.getId())).thenReturn(0L, 1L);

        var result = service("2026-11-01T18:00:00Z").checkIn(7L);

        assertThat(result.completedDays()).isEqualTo(1);
        assertThat(result.points()).isEqualTo(10);
        assertThat(result.status()).isEqualTo("IN_PROGRESS");
        verify(checkIns).save(any());
    }

    @Test
    void missingAnEarlierDayFailsTheChallenge() {
        SevenDayChallenge challenge = new SevenDayChallenge(7L, LocalDate.of(2026, 11, 1));
        when(users.lockById(7L)).thenReturn(Optional.of(new User()));
        when(challenges.findFirstByUserIdOrderByIdDesc(7L)).thenReturn(Optional.of(challenge));
        when(checkIns.countByChallengeId(challenge.getId())).thenReturn(1L);

        assertThat(service("2026-11-03T05:00:00Z").checkIn(7L).status()).isEqualTo("FAILED");
        assertThat(challenge.getStatus()).isEqualTo("FAILED");
    }

    @Test
    void seventhCheckInCompletesChallengeAndAwardsBadge() {
        SevenDayChallenge challenge = new SevenDayChallenge(7L, LocalDate.of(2026, 11, 1));
        when(users.lockById(7L)).thenReturn(Optional.of(new User()));
        when(challenges.findFirstByUserIdOrderByIdDesc(7L)).thenReturn(Optional.of(challenge));
        when(checkIns.countByChallengeId(challenge.getId())).thenReturn(6L, 7L);

        var result = service("2026-11-07T05:00:00Z").checkIn(7L);

        assertThat(result.completedDays()).isEqualTo(7);
        assertThat(result.points()).isEqualTo(70);
        assertThat(result.status()).isEqualTo("COMPLETED");
        verify(badges).save(any(UserBadge.class));
    }

    @Test
    void checkingInTwiceDoesNotAddPointsAgain() {
        SevenDayChallenge challenge = new SevenDayChallenge(7L, LocalDate.of(2026, 11, 2));
        when(users.lockById(7L)).thenReturn(Optional.of(new User()));
        when(challenges.findFirstByUserIdOrderByIdDesc(7L)).thenReturn(Optional.of(challenge));
        when(checkIns.countByChallengeId(challenge.getId())).thenReturn(1L);
        when(checkIns.existsByChallengeIdAndDayNumber(challenge.getId(), 1)).thenReturn(true);

        assertThatThrownBy(() -> service("2026-11-02T05:00:00Z").checkIn(7L))
                .isInstanceOf(AppException.class);
        verify(checkIns, never()).save(any());
    }

    @Test
    void grantingVerificationTwiceKeepsOneActiveBadge() {
        UserBadge existing = new UserBadge(7L, "VERIFIED", java.time.LocalDateTime.of(2026, 11, 1, 9, 0), 1L);
        when(users.lockById(7L)).thenReturn(Optional.of(new User()));
        when(badges.findFirstByUserIdAndBadgeTypeAndRevokedAtIsNull(7L, "VERIFIED"))
                .thenReturn(Optional.of(existing));

        assertThat(service("2026-11-02T05:00:00Z").grantVerification(1L, 7L).type())
                .isEqualTo("VERIFIED");
        verify(badges, never()).save(any());
    }

    @Test
    void revocationStoresReasonAndTime() {
        UserBadge badge = new UserBadge(7L, "ACHIEVEMENT", java.time.LocalDateTime.of(2026, 11, 1, 9, 0), null);
        when(users.lockById(7L)).thenReturn(Optional.of(new User()));
        when(badges.findByIdAndUserId(5L, 7L)).thenReturn(Optional.of(badge));

        var result = service("2026-11-02T05:00:00Z").revoke(1L, 7L, 5L, "Gian lận");

        assertThat(result.revocationReason()).isEqualTo("Gian lận");
        assertThat(result.revokedAt()).isEqualTo(java.time.LocalDateTime.of(2026, 11, 2, 12, 0));
        assertThat(badge.getRevokedBy()).isEqualTo(1L);
        verify(badges).save(badge);
    }
}
