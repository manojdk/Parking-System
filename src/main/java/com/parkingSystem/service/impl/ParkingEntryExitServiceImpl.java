package com.parkingSystem.service.impl;

import com.parkingSystem.model.*;
import com.parkingSystem.repository.ParkingTicketRepository;
import com.parkingSystem.enums.TicketStatus;
import com.parkingSystem.uuidTest.ParkingSpaceRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.List;
import com.parkingSystem.service.IVehicleService;

/**
 * Service for managing vehicle entry and exit in parking system
 */
@Service
@AllArgsConstructor
@Slf4j
public class ParkingEntryExitServiceImpl implements com.parkingSystem.service.IParkingEntryExitService {

    private ParkingTicketRepository parkingTicketRepository;
    private ParkingSpaceRepository parkingSpaceRepository;
    private IVehicleService vehicleService;
    private IUserService userService;

    /**
     * Record vehicle entry into parking
     */
    @Transactional
    public ParkingTicket recordVehicleEntry(Long userId, Long vehicleId, Long parkingSpaceId) {
        try {
            log.info("Recording vehicle entry for user ID: {}, vehicle ID: {}, parking space ID: {}", 
                    userId, vehicleId, parkingSpaceId);

            User user = userService.getUserById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));

            Vehicle vehicle = vehicleService.getVehicleById(vehicleId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

            ParkingSpace parkingSpace = parkingSpaceRepository.findByParkingSpaceId(parkingSpaceId)
                    .orElseThrow(() -> new IllegalArgumentException("Parking space not found"));

            // Check if parking space is available
            if (!"available".equalsIgnoreCase(parkingSpace.getAvailabilityStatus())) {
                throw new IllegalArgumentException("Parking space is not available");
            }

            // Check if there's already an active ticket for this vehicle
            Optional<ParkingTicket> existingActiveTicket = parkingTicketRepository
                    .findActiveTicketByParkingSpaceId(parkingSpaceId);
            if (existingActiveTicket.isPresent()) {
                throw new IllegalArgumentException("This parking space already has an active ticket");
            }

            // Create parking ticket
            ParkingTicket ticket = ParkingTicket.builder()
                    .user(user)
                    .vehicle(vehicle)
                    .parkingSpace(parkingSpace)
                    .entryTime(LocalDateTime.now())
                    .status(TicketStatus.ACTIVE)
                    .build();

            ParkingTicket savedTicket = parkingTicketRepository.save(ticket);

            // Update parking space status
            parkingSpace.setAvailabilityStatus("occupied");
            parkingSpaceRepository.save(parkingSpace);

            log.info("Vehicle entry recorded successfully. Ticket Reference: {}", savedTicket.getTicketReference());
            return savedTicket;
        } catch (Exception e) {
            log.error("Error recording vehicle entry: {}", e.getMessage());
            throw new RuntimeException("Error recording vehicle entry: " + e.getMessage());
        }
    }

    /**
     * Record vehicle exit from parking
     */
    @Transactional
    public ParkingTicket recordVehicleExit(String ticketReference) {
        try {
            log.info("Recording vehicle exit for ticket: {}", ticketReference);

            ParkingTicket ticket = parkingTicketRepository.findByTicketReference(ticketReference)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found with reference: " + ticketReference));

            if (ticket.getStatus() != TicketStatus.ACTIVE) {
                throw new IllegalArgumentException("Ticket is not active. Current status: " + ticket.getStatus());
            }

            // Set exit time
            LocalDateTime exitTime = LocalDateTime.now();
            ticket.setExitTime(exitTime);

            // Calculate duration in minutes
            long durationMinutes = ChronoUnit.MINUTES.between(ticket.getEntryTime(), exitTime);
            ticket.setDurationMinutes(durationMinutes);

            // Update ticket status
            ticket.setStatus(TicketStatus.CLOSED);
            ParkingTicket updatedTicket = parkingTicketRepository.save(ticket);

            // Update parking space status back to available
            ParkingSpace parkingSpace = ticket.getParkingSpace();
            parkingSpace.setAvailabilityStatus("available");
            parkingSpaceRepository.save(parkingSpace);

            log.info("Vehicle exit recorded successfully. Parking duration: {} minutes", durationMinutes);
            return updatedTicket;
        } catch (Exception e) {
            log.error("Error recording vehicle exit: {}", e.getMessage());
            throw new RuntimeException("Error recording vehicle exit: " + e.getMessage());
        }
    }

    /**
     * Get parking ticket details
     */
    public Optional<ParkingTicket> getTicketDetails(String ticketReference) {
        try {
            log.info("Fetching ticket details for reference: {}", ticketReference);
            return parkingTicketRepository.findByTicketReference(ticketReference);
        } catch (Exception e) {
            log.error("Error fetching ticket: {}", e.getMessage());
            throw new RuntimeException("Error fetching ticket: " + e.getMessage());
        }
    }

    /**
     * Get all active tickets (vehicles currently parked)
     */
    public List<ParkingTicket> getActiveTickets() {
        try {
            log.info("Fetching all active parking tickets");
            return parkingTicketRepository.findByStatusAndExitTimeIsNull(TicketStatus.ACTIVE);
        } catch (Exception e) {
            log.error("Error fetching active tickets: {}", e.getMessage());
            throw new RuntimeException("Error fetching active tickets: " + e.getMessage());
        }
    }

    /**
     * Get user's parking history
     */
    public List<ParkingTicket> getUserParkingHistory(Long userId) {
        try {
            log.info("Fetching parking history for user ID: {}", userId);
            return parkingTicketRepository.findByUserIdAndStatus(userId, TicketStatus.CLOSED);
        } catch (Exception e) {
            log.error("Error fetching parking history: {}", e.getMessage());
            throw new RuntimeException("Error fetching parking history: " + e.getMessage());
        }
    }

    /**
     * Get user's current active parking sessions
     */
    public List<ParkingTicket> getUserActiveParking(Long userId) {
        try {
            log.info("Fetching active parking sessions for user ID: {}", userId);
            return parkingTicketRepository.findByUserIdAndStatus(userId, TicketStatus.ACTIVE);
        } catch (Exception e) {
            log.error("Error fetching active parking: {}", e.getMessage());
            throw new RuntimeException("Error fetching active parking: " + e.getMessage());
        }
    }

    /**
     * Get count of currently parked vehicles
     */
    public Long getCurrentOccupancy() {
        try {
            log.info("Calculating current occupancy");
            return parkingTicketRepository.countActiveTickets();
        } catch (Exception e) {
            log.error("Error calculating occupancy: {}", e.getMessage());
            throw new RuntimeException("Error calculating occupancy: " + e.getMessage());
        }
    }

    /**
     * Cancel a parking ticket
     */
    @Transactional
    public ParkingTicket cancelTicket(String ticketReference, String reason) {
        try {
            log.info("Cancelling ticket: {}", ticketReference);

            ParkingTicket ticket = parkingTicketRepository.findByTicketReference(ticketReference)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

            if (ticket.getStatus() == TicketStatus.CANCELLED) {
                throw new IllegalArgumentException("Ticket is already cancelled");
            }

            ticket.setStatus(TicketStatus.CANCELLED);
            ticket.setNotes(reason);

            // If vehicle hasn't exited, mark it as exited
            if (ticket.getExitTime() == null) {
                ticket.setExitTime(LocalDateTime.now());
                long durationMinutes = ChronoUnit.MINUTES.between(ticket.getEntryTime(), ticket.getExitTime());
                ticket.setDurationMinutes(durationMinutes);
                
                // Update parking space
                ParkingSpace parkingSpace = ticket.getParkingSpace();
                parkingSpace.setAvailabilityStatus("available");
                parkingSpaceRepository.save(parkingSpace);
            }

            ParkingTicket cancelledTicket = parkingTicketRepository.save(ticket);
            log.info("Ticket cancelled successfully");
            return cancelledTicket;
        } catch (Exception e) {
            log.error("Error cancelling ticket: {}", e.getMessage());
            throw new RuntimeException("Error cancelling ticket: " + e.getMessage());
        }
    }
}
