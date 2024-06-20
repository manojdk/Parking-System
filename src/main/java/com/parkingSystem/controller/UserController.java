package com.parkingSystem.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.parkingSystem.model.User;
import com.parkingSystem.service.UserService;

import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {

	private UserService userService;

	@PostMapping("/register")
	public ResponseEntity<Object> registerUser(@RequestBody Map<String, String> request) {
		try {
			String userName = request.get("userName");
			String email = request.get("userEmail");
			String password = request.get("password");
			String licensePlate = request.get("licencePlate");
			User registeredUser = userService.registerUser(userName, email, password, licensePlate);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "User registration successful");
			response.put("userId", registeredUser.getUserId());

			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error registering user: " + e.getMessage());
		}
	}

	@GetMapping("/details")
	public ResponseEntity<Object> getUserDetails(@RequestParam Long userId) {
		try {
			Map<String, Object> getUserDetail = userService.getUserDeatils(userId);
			return ResponseEntity.ok(getUserDetail);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error retrieving user details: " + e.getMessage());
		}
	}

	@PutMapping("/update")
	public ResponseEntity<Object> updateUser(@RequestBody Map<String, String> request) {
		try {
			Long userId = Long.parseLong(request.get("userId"));
			String userName = request.get("userName");
			String userEmail = request.get("userEmail");
			String password = request.get("password");
			String licensePlate = request.get("licensePlate");

			User updatedUser = userService.updateUser(userId, userName, userEmail, password, licensePlate);

			Map<String, Object> response = new HashMap<>();
			response.put("status", "success");
			response.put("message", "User updated successful");
			response.put("userId", updatedUser.getUserId());

			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error updating user: " + e.getMessage());
		}
	}
}
