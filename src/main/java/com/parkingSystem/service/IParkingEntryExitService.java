package com.parkingSystem.service;

import com.parkingSystem.model.ParkingTicket;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing vehicle entry and exit in parking system
 */
public interface IParkingEntryExitService {

    /**
     * Record vehicle entry into parking
     */
    ParkingTicket recordVehicleEntry(Long userId, Long vehicleId, Long parkingSpaceId);

    /**
     * Record vehicle exit from parking
     */
    ParkingTicket recordVehicleExit(String ticketReference);

    /**
     * Get parking ticket details
     */
    Optional<ParkingTicket> getTicketDetails(String ticketReference);

    /**
     * Get all active tickets (vehicles currently parked)
     */
    List<ParkingTicket> getActiveTickets();

    /**
     * Get user's parking history
     */
    List<ParkingTicket> getUserParkingHistory(Long userId);

    /**
     * Get user's current active parking sessions
     */
    List<ParkingTicket> getUserActiveParking(Long userId);

    /**
     * Get count of currently parked vehicles
     */
    Long getCurrentOccupancy();

    /**
     * Cancel a parking ticket
     */
    ParkingTicket cancelTicket(String ticketReference, String reason);
}
