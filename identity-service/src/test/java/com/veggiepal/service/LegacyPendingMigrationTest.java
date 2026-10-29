package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.repository.UserRepository;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:legacy_pending;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver","spring.datasource.username=sa","spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop","spring.sql.init.mode=never","spring.jpa.show-sql=false"})
class LegacyPendingMigrationTest {
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    Long pending(String name, String createdAt) {
        Long id=users.saveAndFlush(User.builder().email(name+"@example.com").fullName(name).phone("0123456789")
                .passwordHash("hash").role(Role.USER).status(UserStatus.PENDING).emailVerified(false).build()).getId();
        jdbc.update("update users set created_at = ? where id = ?", createdAt, id);
        return id;
    }

    @Test
    void legacyPendingAccountsBecomeActiveAndOtpEraOnesStayPending() {
        users.deleteAll();
        Long legacy=pending("legacy","2026-10-01 10:00:00");
        Long otpEra=pending("otp","2026-10-29 10:00:00");

        new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource("data.sql")).execute(dataSource); // every start

        assertThat(users.findById(legacy).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(users.findById(legacy).orElseThrow().getEmailVerified()).isFalse();
        assertThat(users.findById(otpEra).orElseThrow().getStatus()).isEqualTo(UserStatus.PENDING);
    }
}
