package com.parkingSystem.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service interface for custom user details
 */
public interface ICustomUserDetailsService extends UserDetailsService {

    /**
     * Load user by username
     *
     * @param username Username
     * @return UserDetails
     */
    UserDetails loadUserByUsername(String username);
}
