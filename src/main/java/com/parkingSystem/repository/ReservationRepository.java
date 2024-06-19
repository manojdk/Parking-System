package com.parkingSystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	List<Reservation> findAllByUserId(Long userId);

	List<Reservation> findAllByParkingSpaceId(Long parkingSpaceId);
}
