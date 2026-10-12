package com.parkingSystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parkingSystem.dto.RegisterUserRequest;
import com.parkingSystem.dto.UpdateUserRequest;
import com.parkingSystem.model.User;
import com.parkingSystem.security.CurrentUserService;
import com.parkingSystem.service.IUserService;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

	private IUserService userService;

	private CurrentUserService currentUserService;

	@PostMapping("/register")
	public ResponseEntity<Object> registerUser(@Valid @RequestBody RegisterUserRequest request) {
		User registeredUser = userService.registerUser(request.getUserName(), request.getUserEmail(),
				request.getPassword(), request.getLicencePlate());

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "User registration successful");
		response.put("userId", registeredUser.getUserId());

		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	@GetMapping("/details")
	public ResponseEntity<Object> getUserDetails(@RequestParam Long userId) {
		currentUserService.checkAccess(userId);
		return ResponseEntity.ok(userService.getUserDetails(userId));
	}

	@PutMapping("/update")
	public ResponseEntity<Object> updateUser(@Valid @RequestBody UpdateUserRequest request) {
		currentUserService.checkAccess(request.getUserId());

		// Passwords can only be changed by the account owner, not by other users (admins included)
		if (request.getPassword() != null
				&& !request.getUserId().equals(currentUserService.getCurrentUser().getUserId())) {
			throw new AccessDeniedException("Only the account owner can change the password");
		}

		User updatedUser = userService.updateUser(request.getUserId(), request.getUserName(),
				request.getUserEmail(), request.getPassword(), request.getLicensePlate());

		Map<String, Object> response = new HashMap<>();
		response.put("status", "success");
		response.put("message", "User updated successfully");
		response.put("userId", updatedUser.getUserId());

		return ResponseEntity.ok(response);
	}
}
