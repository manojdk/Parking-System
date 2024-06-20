package com.parkingSystem.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	User findByUserName(String userName);

	User findByUserEmail(String userEmail);

	Boolean existsByUserName(String userName);

	Boolean existsByUserEmail(String userEmail);

	Optional<User> findByUserId(Long userId);
}
