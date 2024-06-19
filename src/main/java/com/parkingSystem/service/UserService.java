package com.parkingSystem.service;

import java.time.LocalDateTime;
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
	public User registerUser(User user) {
		try {
			if (userRepository.findByUserName(user.getUserName()) != null) {
				throw new IllegalArgumentException("Username already exists");
			}

			if (userRepository.findByUserEmail(user.getUserEmail()) != null) {
				throw new IllegalArgumentException("User email already exists");
			}

			user.setCreatedDate(LocalDateTime.now());
			return userRepository.save(user);
		} catch (DataAccessException e) {
			throw new RuntimeException("Database error occurred while registering user: " + e.getMessage());

		} catch (IllegalArgumentException e) {
			throw e;
		}
	}

	public Optional<User> getUserById(Long userId) {
		try {
			return userRepository.findById(userId);
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
