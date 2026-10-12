package com.parkingSystem.service.impl;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.enums.TicketStatus;
import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.model.ParkingTicket;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.model.Vehicle;
import com.parkingSystem.repository.ParkingTicketRepository;
import com.parkingSystem.repository.ReservationRepository;
import com.parkingSystem.service.IBillingService;
import com.parkingSystem.service.IParkingEntryExitService;
import com.parkingSystem.service.IUserService;
import com.parkingSystem.service.IVehicleService;
import com.parkingSystem.repository.ParkingSpaceRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing vehicle entry and exit in parking system.
 * <p>
 * Concurrency: entry, exit and cancel lock the parking-space row first (SELECT ... FOR UPDATE) and only
 * then read or change tickets and reservations for it, the same order the reservation service uses. Two
 * cars trying to take the same space are therefore handled one after the other: the second one sees the
 * space as OCCUPIED and gets a 409. The unique "active" columns on tickets (V3 migration) are the
 * database-level safety net behind the lock.
 */
@Service
@AllArgsConstructor
@Slf4j
public class ParkingEntryExitServiceImpl implements IParkingEntryExitService {

    private ParkingTicketRepository parkingTicketRepository;
    private ParkingSpaceRepository parkingSpaceRepository;
    private ReservationRepository reservationRepository;
    private IVehicleService vehicleService;
    private IUserService userService;
    private IBillingService billingService;
    private Clock clock;

    /**
     * Record vehicle entry into parking.
     * <p>
     * The vehicle must fit the space's slot type (bikes in bike bays, only EVs in charging bays).
     * An AVAILABLE space takes any driver. A RESERVED space only takes the driver who holds the
     * reservation, which is then marked FULFILLED and linked to the new ticket. If the hold has already run
     * out (the expiry job may not have run yet) the reservation is expired here and the space is treated as
     * AVAILABLE.
     */
    @Transactional
    public ParkingTicket recordVehicleEntry(Long userId, Long vehicleId, Long parkingSpaceId) {
        log.info("Recording vehicle entry for user ID: {}, vehicle ID: {}, parking space ID: {}",
                userId, vehicleId, parkingSpaceId);

        // Lock the space before anything else reads it
        ParkingSpace parkingSpace = parkingSpaceRepository.findByIdForUpdate(parkingSpaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found with ID: " + parkingSpaceId));

        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        Vehicle vehicle = vehicleService.getVehicleById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + vehicleId));
        if (vehicle.getUser() == null || !user.getUserId().equals(vehicle.getUser().getUserId())) {
            throw new BadRequestException("Vehicle does not belong to this user");
        }
        if (!Boolean.TRUE.equals(vehicle.getIsActive())) {
            throw new BadRequestException("Vehicle is deactivated");
        }
        if (!Boolean.TRUE.equals(parkingSpace.getIsActive())) {
            throw new BadRequestException("Parking space is not in service");
        }
        if (!parkingSpace.getSlotType().accepts(vehicle.getVehicleType())) {
            throw new BadRequestException("A " + vehicle.getVehicleType() + " cannot park in a "
                    + parkingSpace.getSlotType() + " space");
        }

        LocalDateTime now = now();
        Reservation reservation = claimSpace(parkingSpace, userId, now);

        // A vehicle can only be parked in one place at a time
        if (parkingTicketRepository.existsByVehicleVehicleIdAndStatus(vehicleId, TicketStatus.ACTIVE)) {
            throw new ConflictException("Vehicle already has an active parking ticket");
        }

        ParkingTicket ticket = ParkingTicket.builder()
                .user(user)
                .vehicle(vehicle)
                .parkingSpace(parkingSpace)
                .entryTime(now)
                .status(TicketStatus.ACTIVE)
                .build();
        ParkingTicket savedTicket = parkingTicketRepository.save(ticket);

        parkingSpace.setAvailabilityStatus(SpaceStatus.OCCUPIED);
        parkingSpaceRepository.save(parkingSpace);

        if (reservation != null) {
            reservation.fulfil(savedTicket, now);
            reservationRepository.save(reservation);
            log.info("Reservation {} fulfilled by ticket {}", reservation.getReservationId(),
                    savedTicket.getTicketReference());
        }

        log.info("Vehicle entry recorded successfully. Ticket Reference: {}", savedTicket.getTicketReference());
        return savedTicket;
    }

    /**
     * Decides whether this user may park in the (locked) space now.
     *
     * @return the user's reservation for this space, to be fulfilled, or null for a walk-in
     */
    private Reservation claimSpace(ParkingSpace parkingSpace, Long userId, LocalDateTime now) {
        switch (parkingSpace.getAvailabilityStatus()) {
            case AVAILABLE:
                return null;
            case OCCUPIED:
                throw new ConflictException("Parking space is not available");
            case OUT_OF_SERVICE:
                throw new ConflictException("Parking space is out of service");
            case RESERVED:
                break;
            default:
                throw new IllegalStateException("Unhandled space status " + parkingSpace.getAvailabilityStatus());
        }

        Optional<Reservation> held = reservationRepository
                .findActiveBySpaceIdForUpdate(parkingSpace.getParkingSpaceId());
        if (held.isEmpty()) {
            // RESERVED without an open reservation should not happen; repair it rather than block the space
            log.warn("Space {} was RESERVED without an active reservation; treating it as available",
                    parkingSpace.getParkingSpaceId());
            return null;
        }
        Reservation reservation = held.get();
        if (reservation.isHoldOver(now)) {
            reservation.expire();
            reservationRepository.save(reservation);
            log.info("Reservation {} expired at the gate (no-show), space {} released",
                    reservation.getReservationId(), parkingSpace.getParkingSpaceId());
            return null;
        }
        if (!reservation.getUser().getUserId().equals(userId)) {
            throw new ConflictException("Parking space is reserved for another driver");
        }
        return reservation;
    }

    /**
     * Record vehicle exit from parking.
     * <p>
     * Design choice: the hourly bill is generated inside the same transaction as the exit.
     * If billing fails, the whole exit is rolled back and the error is returned to the caller,
     * so a vehicle can never leave without a bill being recorded and the exit can simply be
     * retried. (Previously the billing error was swallowed while the shared transaction was
     * marked rollback-only, which silently undid the exit.)
     */
    @Transactional
    public ParkingTicket recordVehicleExit(String ticketReference) {
        log.info("Recording vehicle exit for ticket: {}", ticketReference);

        ParkingTicket ticket = lockTicket(ticketReference);
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new ConflictException("Ticket is not active. Current status: " + ticket.getStatus());
        }

        LocalDateTime exitTime = now();
        ticket.setExitTime(exitTime);
        long durationMinutes = ChronoUnit.MINUTES.between(ticket.getEntryTime(), exitTime);
        ticket.setDurationMinutes(durationMinutes);
        ticket.setStatus(TicketStatus.CLOSED);
        ParkingTicket updatedTicket = parkingTicketRepository.save(ticket);

        freeSpace(ticket.getParkingSpace());

        // Generate bill for the closed ticket (failure rolls back the exit, see method doc)
        billingService.generateBill(updatedTicket.getTicketId());

        log.info("Vehicle exit recorded successfully. Parking duration: {} minutes", durationMinutes);
        return updatedTicket;
    }

    /**
     * Get parking ticket details
     */
    public Optional<ParkingTicket> getTicketDetails(String ticketReference) {
        log.info("Fetching ticket details for reference: {}", ticketReference);
        return parkingTicketRepository.findByTicketReference(ticketReference);
    }

    /**
     * Get all active tickets (vehicles currently parked)
     */
    public Page<ParkingTicket> getActiveTickets(Pageable pageable) {
        log.info("Fetching all active parking tickets");
        return parkingTicketRepository.findByStatusAndExitTimeIsNull(TicketStatus.ACTIVE, pageable);
    }

    /**
     * Get user's parking history
     */
    public Page<ParkingTicket> getUserParkingHistory(Long userId, Pageable pageable) {
        log.info("Fetching parking history for user ID: {}", userId);
        return parkingTicketRepository.findByUserUserIdAndStatus(userId, TicketStatus.CLOSED, pageable);
    }

    /**
     * Get user's current active parking sessions
     */
    public List<ParkingTicket> getUserActiveParking(Long userId) {
        log.info("Fetching active parking sessions for user ID: {}", userId);
        return parkingTicketRepository.findByUserIdAndStatus(userId, TicketStatus.ACTIVE);
    }

    /**
     * Get count of currently parked vehicles
     */
    public Long getCurrentOccupancy() {
        log.info("Calculating current occupancy");
        return parkingTicketRepository.countActiveTickets();
    }

    /**
     * Cancel an active parking ticket and release its parking space. No bill is generated.
     */
    @Transactional
    public ParkingTicket cancelTicket(String ticketReference, String reason) {
        log.info("Cancelling ticket: {}", ticketReference);

        ParkingTicket ticket = lockTicket(ticketReference);
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            throw new ConflictException("Only active tickets can be cancelled. Current status: " + ticket.getStatus());
        }

        ticket.setStatus(TicketStatus.CANCELLED);
        ticket.setNotes(reason);
        if (ticket.getExitTime() == null) {
            ticket.setExitTime(now());
            long durationMinutes = ChronoUnit.MINUTES.between(ticket.getEntryTime(), ticket.getExitTime());
            ticket.setDurationMinutes(durationMinutes);
        }
        freeSpace(ticket.getParkingSpace());

        ParkingTicket cancelledTicket = parkingTicketRepository.save(ticket);
        log.info("Ticket cancelled successfully");
        return cancelledTicket;
    }

    /**
     * Locks the ticket's space and then the ticket (same lock order as entry), and returns the ticket.
     */
    private ParkingTicket lockTicket(String ticketReference) {
        Long spaceId = parkingTicketRepository.findSpaceIdByTicketReference(ticketReference)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with reference: " + ticketReference));
        parkingSpaceRepository.findByIdForUpdate(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Parking space not found with ID: " + spaceId));
        return parkingTicketRepository.findByTicketReferenceForUpdate(ticketReference)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with reference: " + ticketReference));
    }

    /** Truncated to the database's precision, so values returned now equal the values read back later. */
    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MICROS);
    }

    /** The car has left: an OCCUPIED space becomes AVAILABLE. An admin-set OUT_OF_SERVICE status is kept. */
    private void freeSpace(ParkingSpace parkingSpace) {
        if (parkingSpace.getAvailabilityStatus() == SpaceStatus.OCCUPIED) {
            parkingSpace.setAvailabilityStatus(SpaceStatus.AVAILABLE);
            parkingSpaceRepository.save(parkingSpace);
        }
    }
}
