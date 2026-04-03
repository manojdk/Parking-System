package com.parkingSystem.controller;

import com.parkingSystem.config.JwtUtil;
import com.parkingSystem.dto.AuthResponse;
import com.parkingSystem.dto.LoginRequest;
import com.parkingSystem.model.User;
import com.parkingSystem.service.IUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final IUserService userService;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil, IUserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            User user = userService.getUserByUsername(request.getUsername());
            
            if (user == null) {
                return ResponseEntity.status(401).body("User not found after authentication");
            }

            String token = jwtUtil.generateToken(user.getUserName(), user.getRole());

            return ResponseEntity.ok(new AuthResponse(token, user.getRole()));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }
}
