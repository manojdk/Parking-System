package com.parkingSystem.service.impl;

import com.parkingSystem.dto.UserDetailsDTO;
import java.sql.Date;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
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
		// Check if the username or email already exists
		if (userRepository.existsByUserName(userName)) {
			throw new ConflictException("Username already exists.");
		}
		if (userRepository.existsByUserEmail(email)) {
			throw new ConflictException("Email already exists.");
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
	}

	/**
	 * Get user detail by userId
	 * 
	 * @param userId User ID
	 * @return User data
	 */
	public UserDetailsDTO getUserDetails(Long userId) {
		User user = getUserById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
		return UserDetailsDTO.from(user);
	}

	/**
	 * Update user data. Null fields are left unchanged; the role is never changed here.
	 * Changing the user name invalidates existing JWTs (they are bound to the user name).
	 * 
	 * @param userId       User ID
	 * @param userName     User name
	 * @param userEmail    User Email
	 * @param password
	 * @param licensePlate License no. of vehicle
	 * @return Updated user data saved
	 */
	public User updateUser(Long userId, String userName, String userEmail, String password, String licensePlate) {
		User user = userRepository.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

		if (userName != null && !userName.equals(user.getUserName())) {
			if (userRepository.existsByUserName(userName)) {
				throw new ConflictException("Username already exists.");
			}
			user.setUserName(userName);
		}
		if (userEmail != null && !userEmail.equals(user.getUserEmail())) {
			if (userRepository.existsByUserEmail(userEmail)) {
				throw new ConflictException("Email already exists.");
			}
			user.setUserEmail(userEmail);
		}
		if (password != null)
			user.setPassword(passwordEncoder.encode(password));
		if (licensePlate != null)
			user.setLicencePlate(licensePlate);

		return userRepository.save(user);
	}

	/**
	 * Get user data by userId helper class
	 * 
	 * @param userId User ID
	 * @return user data by Id
	 */
	public Optional<User> getUserById(Long userId) {
		return userRepository.findByUserId(userId);
	}

	/**
	 * Get User details by User name and User email
	 * 
	 * @param username  User name
	 * @param userEmail User email
	 * @return User detail
	 */
	public Optional<List<User>> getUserByUsername(String username, String userEmail) {
		return Optional.ofNullable(userRepository.findByUserNameAndUserEmail(username, userEmail));
	}

	public User getUserByUsername(String username) {
		return userRepository.findByUserName(username)
				.orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
	}

	/**
	 * Get user by email
	 */
	public Optional<User> getUserByEmail(String email) {
		return userRepository.findByUserEmail(email);
	}

	/**
	 * Get all users
	 */
	public List<User> getAllUsers() {
		return userRepository.findAll();
	}

	/**
	 * Delete user
	 */
	public void deleteUser(Long userId) {
		userRepository.deleteById(userId);
	}

	/**
	 * Check if user exists by username
	 */
	public boolean existsByUserName(String userName) {
		return userRepository.existsByUserName(userName);
	}

	/**
	 * Check if user exists by email
	 */
	public boolean existsByUserEmail(String email) {
		return userRepository.existsByUserEmail(email);
	}

}
