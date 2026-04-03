package com.parkingSystem.service.impl;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.dao.DataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.parkingSystem.model.User;
import com.parkingSystem.repository.UserRepository;
import com.parkingSystem.service.IUserService;

import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class UserServiceImpl implements IUserService {

	private UserRepository userRepository;

	private PasswordEncoder passwordEncoder;

	/**
	 * Register new user
	 * 
	 * @param userName     User name
	 * @param email        User email
	 * @param password
	 * @param licensePlate License no. of vehicle
	 * @return User data to repository
	 */
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
			user.setPassword(passwordEncoder.encode(password));
			user.setLicencePlate(licensePlate);
			user.setCreatedDate(LocalDateTime.now());
			user.setRegistrationDate(new Date(System.currentTimeMillis()));

			return userRepository.save(user);
		} catch (DataAccessException e) {
			log.error("error : ", e);
			throw new RuntimeException("Database error occurred while registering user: " + e.getMessage());

		} catch (IllegalArgumentException e) {
			log.error("error : ", e);
			throw e;
		}
	}

	/**
	 * Get user detail by userId
	 * 
	 * @param userId User ID
	 * @return User data
	 */
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

	/**
	 * Update user data
	 * 
	 * @param userId       User ID
	 * @param userName     User name
	 * @param userEmail    User Email
	 * @param password
	 * @param licensePlate License no. of vehicle
	 * @return Updated user data saved
	 */
	public User updateUser(Long userId, String userName, String userEmail, String password, String licensePlate) {
		try {
			Optional<User> userOptional = userRepository.findByUserId(userId);

			if (userOptional.isEmpty()) {
				throw new IllegalArgumentException("User not found with ID: " + userId);
			}

			User user = userOptional.get();
			if (userName != null)
				user.setUserName(userName);
			if (userEmail != null)
				user.setUserEmail(userEmail);
			if (password != null)
				user.setPassword(passwordEncoder.encode(password));
			if (licensePlate != null)
				user.setLicencePlate(licensePlate);

			return userRepository.save(user);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while updating user: " + e.getMessage());
		}
	}

	/**
	 * Get user data by userId helper class
	 * 
	 * @param userId User ID
	 * @return user data by Id
	 */
	public Optional<User> getUserById(Long userId) {
		try {
			return userRepository.findByUserId(userId);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching user by ID: " + e.getMessage());
		}
	}

	/**
	 * Get User details by User name and User email
	 * 
	 * @param username  User name
	 * @param userEmail User email
	 * @return User detail
	 */
	public Optional<List<User>> getUserByUsername(String username, String userEmail) {
		try {
			return Optional.ofNullable(userRepository.findByUserNameAndUserEmail(username, userEmail));
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching user by Name" + e.getMessage());
		}
	}

	public User getUserByUsername(String username) {
		return userRepository.findByUserName(username)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));
	}

	/**
	 * Get user by email
	 */
	public Optional<User> getUserByEmail(String email) {
		try {
			return userRepository.findByUserEmail(email);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching user by email: " + e.getMessage());
		}
	}

	/**
	 * Get all users
	 */
	public List<User> getAllUsers() {
		try {
			return userRepository.findAll();
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while fetching all users: " + e.getMessage());
		}
	}

	/**
	 * Delete user
	 */
	public void deleteUser(Long userId) {
		try {
			userRepository.deleteById(userId);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while deleting user: " + e.getMessage());
		}
	}

	/**
	 * Check if user exists by username
	 */
	public boolean existsByUserName(String userName) {
		try {
			return userRepository.existsByUserName(userName);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while checking username existence: " + e.getMessage());
		}
	}

	/**
	 * Check if user exists by email
	 */
	public boolean existsByUserEmail(String email) {
		try {
			return userRepository.existsByUserEmail(email);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while checking email existence: " + e.getMessage());
		}
	}

}
