package com.parkingSystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.Feedback;
import com.parkingSystem.model.User;
import com.parkingSystem.model.Reservation;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
	List<Feedback> findAllByUser(User user);

	List<Feedback> findAllByReservation(Reservation reservation);

	List<Feedback> findAllByUserUserId(Long userId);

	boolean existsByReservationReservationId(Long reservationId);

	Feedback findByFeedbackId(Long feedbackId);

	void deleteByFeedbackId(Long feedbackId);

	// ---- paginated (step 6)

	Page<Feedback> findAllByUserUserId(Long userId, Pageable pageable);
}
