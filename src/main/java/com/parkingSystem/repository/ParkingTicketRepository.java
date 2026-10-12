package com.parkingSystem.repository;

import com.parkingSystem.model.ParkingTicket;
import com.parkingSystem.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ParkingTicket entity
 */
@Repository
public interface ParkingTicketRepository extends JpaRepository<ParkingTicket, Long> {

    Optional<ParkingTicket> findByTicketReference(String ticketReference);

    List<ParkingTicket> findByUserUserId(Long userId);

    List<ParkingTicket> findByStatus(TicketStatus status);

    List<ParkingTicket> findByStatusAndExitTimeIsNull(TicketStatus status);

    @Query("SELECT pt FROM ParkingTicket pt WHERE pt.user.userId = ?1 AND pt.status = ?2")
    List<ParkingTicket> findByUserIdAndStatus(Long userId, TicketStatus status);

    @Query("SELECT pt FROM ParkingTicket pt WHERE pt.entryTime BETWEEN ?1 AND ?2")
    List<ParkingTicket> findTicketsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(pt) FROM ParkingTicket pt WHERE pt.status = 'ACTIVE' AND pt.exitTime IS NULL")
    Long countActiveTickets();

    @Query("SELECT pt FROM ParkingTicket pt WHERE pt.parkingSpace.parkingSpaceId = ?1 AND pt.status = 'ACTIVE'")
    Optional<ParkingTicket> findActiveTicketByParkingSpaceId(Long parkingSpaceId);

    boolean existsByVehicleVehicleIdAndStatus(Long vehicleId, TicketStatus status);

    boolean existsByParkingSpaceParkingSpaceId(Long parkingSpaceId);

    /** The space a ticket is for, read without loading the ticket (lock the space first, then the ticket). */
    @Query("SELECT pt.parkingSpace.parkingSpaceId FROM ParkingTicket pt WHERE pt.ticketReference = :reference")
    Optional<Long> findSpaceIdByTicketReference(@Param("reference") String ticketReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pt FROM ParkingTicket pt WHERE pt.ticketReference = :reference")
    Optional<ParkingTicket> findByTicketReferenceForUpdate(@Param("reference") String ticketReference);
}

