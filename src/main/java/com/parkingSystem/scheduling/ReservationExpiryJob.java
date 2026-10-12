package com.parkingSystem.scheduling;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.parkingSystem.service.IReservationService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Expires no-show reservations and frees their spaces.
 * <p>
 * Each reservation is expired in its own transaction, so one failure does not undo the others. Running
 * several app instances is safe: expiry locks the space and reservation rows and re-checks the status, so a
 * reservation that another instance (or a driver arriving at the gate) already handled is skipped.
 */
@Component
@AllArgsConstructor
@Slf4j
public class ReservationExpiryJob {

	private IReservationService reservationService;

	@Scheduled(fixedDelayString = "${app.reservation.expiry-check-ms:60000}",
			initialDelayString = "${app.reservation.expiry-check-ms:60000}")
	public void run() {
		expireOverdueReservations();
	}

	/**
	 * @return number of reservations expired in this run
	 */
	public int expireOverdueReservations() {
		List<Long> overdue = reservationService.findOverdueReservationIds();
		int expired = 0;
		for (Long reservationId : overdue) {
			try {
				if (reservationService.expireReservation(reservationId)) {
					expired++;
				}
			} catch (RuntimeException ex) {
				log.error("Could not expire reservation {}", reservationId, ex);
			}
		}
		if (expired > 0) {
			log.info("Expired {} no-show reservation(s)", expired);
		}
		return expired;
	}
}
