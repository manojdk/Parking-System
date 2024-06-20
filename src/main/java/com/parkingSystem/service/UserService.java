package com.parkingSystem.service;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.parkingSystem.model.User;
import com.parkingSystem.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {

	private UserRepository userRepository;

	@Transactional
	public User registerUser(String userName, String email, String password, String licensePlate) {
		try {
			// Check if the username or email already exists
			if (userRepository.existsByUserName(userName)) {
				throw new IllegalArgumentException("Username already exists.");
			}
			if (userRepository.existsByUserEmail(email)) {
				throw new IllegalArgumentException("Email already exists.");
			}

			// Create and save the user
			User user = new User();
			user.setUserName(userName);
			user.setUserEmail(email);
			user.setPassword(password);
			user.setLicencePlate(licensePlate);
			user.setCreatedDate(LocalDateTime.now());
			user.setRegistrationDate(new Date(System.currentTimeMillis()));

			return userRepository.save(user);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while registering user: " + e.getMessage());

		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public Map<String, Object> getUserDeatils(Long userId) {
		try {
			Optional<User> userOptional = getUserById(userId);

			if (userOptional.isEmpty()) {
				throw new IllegalArgumentException("User not found with ID: " + userId);
			}

			User user = userOptional.get();
			Map<String, Object> profile = new HashMap<>();
			profile.put("licensePlate", user.getLicencePlate());
			profile.put("createdDate", user.getCreatedDate());

			Map<String, Object> userProfile = new LinkedHashMap<>();
			userProfile.put("userId", user.getUserId());
			userProfile.put("userName", user.getUserName());
			userProfile.put("userEmail", user.getUserEmail());
			userProfile.put("registrationDate", user.getRegistrationDate());
			userProfile.put("profile", profile);

			return userProfile;
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching user details: " + e.getMessage());
		}
	}

	public Optional<User> getUserById(Long userId) {
		try {
			return userRepository.findByUserId(userId);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching user by ID: " + e.getMessage());
		}
	}

	public Optional<User> getUserByUsername(String username) {
		try {
			return Optional.ofNullable(userRepository.findByUserName(username));
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching user by Name" + e.getMessage());
		}
	}

	public Optional<User> getUserByUseremail(String email) {
		try {
			return Optional.ofNullable(userRepository.findByUserEmail(email));
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching user by Name" + e.getMessage());
		}
	}
}
