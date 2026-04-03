package com.parkingSystem.service;

import com.parkingSystem.model.User;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing users
 */
public interface IUserService {

    /**
     * Register new user
     *
     * @param userName     User name
     * @param email        User email
     * @param password
     * @param licensePlate License no. of vehicle
     * @return User data to repository
     */
    User registerUser(String userName, String email, String password, String licensePlate);

    /**
     * Get user details by userId
     *
     * @param userId User ID
     * @return User data
     */
    Map<String, Object> getUserDeatils(Long userId);

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
    User updateUser(Long userId, String userName, String userEmail, String password, String licensePlate);

    /**
     * Get user data by userId helper class
     *
     * @param userId User ID
     * @return user data by Id
     */
    Optional<User> getUserById(Long userId);

    /**
     * Get User details by User name and User email
     *
     * @param username  User name
     * @param userEmail User email
     * @return User detail
     */
    Optional<List<User>> getUserByUsername(String username, String userEmail);

    /**
     * Get user by username
     *
     * @param username Username
     * @return User
     */
    User getUserByUsername(String username);

    /**
     * Get user by email
     *
     * @param email User email
     * @return User data
     */
    Optional<User> getUserByEmail(String email);

    /**
     * Get all users
     *
     * @return List of users
     */
    List<User> getAllUsers();

    /**
     * Delete user
     *
     * @param userId User ID
     */
    void deleteUser(Long userId);

    /**
     * Check if user exists by username
     *
     * @param userName Username
     * @return true if exists
     */
    boolean existsByUserName(String userName);

    /**
     * Check if user exists by email
     *
     * @param email Email
     * @return true if exists
     */
    boolean existsByUserEmail(String email);
}
