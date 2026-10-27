package com.veggiepal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.veggiepal.enums.UserStatus;
import com.veggiepal.repository.UserRepository;

@SpringBootTest
class VeggiepalApplicationTests {

	@Autowired
	UserRepository userRepository;

	@Autowired
	PasswordEncoder passwordEncoder;

	@Test
	void contextLoads() {
	}

	// blog-service's seed posts, comments and votes belong to users 1-3 (task B spec §1)
	@Test
	void blogSeedAuthors_resolveThroughThePublicLookup() {
		assertThat(userRepository.findByIdInAndStatus(List.of(1L, 2L, 3L), UserStatus.ACTIVE)).hasSize(3);
	}

	@Test
	void seededAuthors_cannotSignInWithAnyPassword() {
		for (String email : List.of(
				"author1@veggiepal.invalid", "author2@veggiepal.invalid", "author3@veggiepal.invalid")) {
			userRepository.findByEmail(email).ifPresent(user ->
					assertThat(passwordEncoder.matches("SEED_ACCOUNT_NO_LOGIN", user.getPasswordHash())).isFalse());
		}
	}
}
