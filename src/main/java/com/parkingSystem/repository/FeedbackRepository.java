package com.parkingSystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.Feedback;
import com.parkingSystem.model.User;
import com.parkingSystem.model.Reservation;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
	List<Feedback> findAllByUser(User user);

	List<Feedback> findAllByReservation(Reservation reservation);

	Feedback findByFeedbackId(Long feedbackId);

	void deleteByFeedbackId(Long feedbackId);
}
