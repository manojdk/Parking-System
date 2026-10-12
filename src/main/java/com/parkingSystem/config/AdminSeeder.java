package com.parkingSystem.config;

import java.sql.Date;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.parkingSystem.model.User;
import com.parkingSystem.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Creates an initial ADMIN account on startup when ADMIN_USERNAME, ADMIN_EMAIL and
 * ADMIN_PASSWORD are all set. Does nothing if any of them is missing or the account exists.
 */
@Component
@Slf4j
public class AdminSeeder implements ApplicationRunner {

	static final String ADMIN_ROLE = "ADMIN";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final String username;
	private final String email;
	private final String password;

	public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder,
			@Value("${ADMIN_USERNAME:}") String username,
			@Value("${ADMIN_EMAIL:}") String email,
			@Value("${ADMIN_PASSWORD:}") String password) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.username = username;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (isBlank(username) || isBlank(email) || isBlank(password)) {
			return;
		}
		if (userRepository.existsByUserName(username) || userRepository.existsByUserEmail(email)) {
			log.info("Admin seeding skipped: a user with the configured username or email already exists");
			return;
		}

		User admin = new User();
		admin.setUserName(username);
		admin.setUserEmail(email);
		admin.setPassword(passwordEncoder.encode(password));
		admin.setLicencePlate("N/A");
		admin.setRole(ADMIN_ROLE);
		admin.setCreatedDate(LocalDateTime.now());
		admin.setRegistrationDate(new Date(System.currentTimeMillis()));
		userRepository.save(admin);
		log.info("Initial admin account '{}' created", username);
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
