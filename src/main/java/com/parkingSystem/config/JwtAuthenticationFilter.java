package com.parkingSystem.config;

import com.parkingSystem.security.ErrorResponseWriter;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ErrorResponseWriter errorResponseWriter;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, ErrorResponseWriter errorResponseWriter) {
        this.jwtUtil = jwtUtil;
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = authHeader.substring(7);
            String username;
            String role;
            try {
                // Parsing verifies the signature and the expiry date.
                username = jwtUtil.extractUsername(token);
                role = jwtUtil.extractRole(token);
            } catch (ExpiredJwtException e) {
                errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Token has expired");
                return;
            } catch (JwtException | IllegalArgumentException e) {
                errorResponseWriter.write(request, response, HttpStatus.UNAUTHORIZED, "Invalid token");
                return;
            }

            if (username != null && role != null) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
