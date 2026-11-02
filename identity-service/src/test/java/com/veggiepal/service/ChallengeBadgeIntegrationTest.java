package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:challenge_badges;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "spring.jpa.show-sql=false"
})
class ChallengeBadgeIntegrationTest {
    @Autowired ChallengeBadgeService service;
    @Autowired UserRepository users;

    @Test
    @Transactional
    void joinCheckInAndBadgeRevocationPersistInSeparateTables() {
        User user = users.saveAndFlush(User.builder().email("challenge@example.test")
                .passwordHash("unused").fullName("Challenge User").role(Role.USER)
                .status(UserStatus.ACTIVE).emailVerified(true).build());

        var joined = service.join(user.getId());
        var progress = service.checkIn(user.getId());
        var verified = service.grantVerification(user.getId(), user.getId());
        var revoked = service.revoke(user.getId(), user.getId(), verified.id(), "Granted by mistake");

        assertThat(joined.completedDays()).isZero();
        assertThat(progress.completedDays()).isEqualTo(1);
        assertThat(progress.points()).isEqualTo(10);
        assertThatThrownBy(() -> service.checkIn(user.getId())).isInstanceOf(AppException.class);
        assertThat(revoked.revocationReason()).isEqualTo("Granted by mistake");
        assertThat(service.activeBadges(user.getId())).isEmpty();
        assertThat(service.badgeHistory(user.getId())).hasSize(1);
    }
}
