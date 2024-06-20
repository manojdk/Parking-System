package com.parkingSystem.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {
	List<Reservation> findAllByUser(User user);

	List<Reservation> findAllByParkingSpace(ParkingSpace parkingSpace);

	Reservation findByReservationId(Long reservationId);

	//Optional<Reservation> findByReservationId1(Long reservationId);
}
