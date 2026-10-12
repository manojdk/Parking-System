package com.parkingSystem.controller;

import com.parkingSystem.config.JwtUtil;
import com.parkingSystem.dto.AuthResponse;
import com.parkingSystem.dto.LoginRequest;
import com.parkingSystem.model.User;
import com.parkingSystem.service.IUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    /**
     * Authenticates the user and returns a JWT. Bad credentials are answered with 401
     * by the global exception handler.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userService.getUserByUsername(request.getUsername());
        String token = jwtUtil.generateToken(user.getUserName(), user.getRole());

        return ResponseEntity.ok(new AuthResponse(token, user.getRole(), user.getUserId()));
    }
}
