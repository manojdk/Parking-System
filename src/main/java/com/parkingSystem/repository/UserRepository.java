package com.parkingSystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

	List<User> findByUserNameAndUserEmail(String userName, String userEmail);

	Boolean existsByUserName(String userName);

	Boolean existsByUserEmail(String userEmail);

	Optional<User> findByUserId(Long userId);

}
