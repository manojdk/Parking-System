package com.parkingSystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.Feedback;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
	List<Feedback> findAllByUserId(Long userId);

	List<Feedback> findAllByReservationId(Long reservationId);
}
