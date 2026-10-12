package com.parkingSystem.controller;

import org.springframework.data.domain.Sort;
import com.parkingSystem.dto.PageResponse;
import com.parkingSystem.controller.support.Pagination;
import com.parkingSystem.dto.BillDTO;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.security.CurrentUserService;
import com.parkingSystem.service.IBillingService;
import com.parkingSystem.service.IParkingEntryExitService;
import com.parkingSystem.model.ParkingTicket;
import com.parkingSystem.dto.VehicleEntryRequest;
import com.parkingSystem.dto.ParkingTicketDTO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Vehicle Entry/Exit management
 */
@RestController
@RequestMapping("/api/parking")
@AllArgsConstructor
@Slf4j
public class ParkingEntryExitController {

    private IParkingEntryExitService parkingEntryExitService;
    private IBillingService billingService;
    private CurrentUserService currentUserService;

    /**
     * Record vehicle entry
     */
    @PostMapping("/entry")
    public ResponseEntity<?> recordVehicleEntry(@Valid @RequestBody VehicleEntryRequest request) {
        log.info("Vehicle entry request received");
        Long userId = currentUserService.resolveUserId(request.getUserId());
        ParkingTicket ticket = parkingEntryExitService.recordVehicleEntry(
                userId,
                request.getVehicleId(),
                request.getParkingSpaceId()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Vehicle entry recorded successfully");
        response.put("ticket", convertToDTO(ticket));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Record vehicle exit. The hourly bill is generated as part of the exit and returned.
     */
    @PostMapping("/exit")
    public ResponseEntity<?> recordVehicleExit(@RequestParam String ticketReference) {
        log.info("Vehicle exit request received for ticket: {}", ticketReference);
        currentUserService.checkAccess(findTicket(ticketReference).getUser().getUserId());

        ParkingTicket ticket = parkingEntryExitService.recordVehicleExit(ticketReference);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Vehicle exit recorded successfully");
        response.put("ticket", convertToDTO(ticket));
        billingService.getBillForTicket(ticket.getTicketId())
                .ifPresent(bill -> response.put("bill", BillDTO.from(bill)));
        return ResponseEntity.ok(response);
    }

    /**
     * Get ticket details
     */
    @GetMapping("/ticket/{ticketReference}")
    public ResponseEntity<?> getTicketDetails(@PathVariable String ticketReference) {
        ParkingTicket ticket = findTicket(ticketReference);
        currentUserService.checkAccess(ticket.getUser().getUserId());
        return ResponseEntity.ok(convertToDTO(ticket));
    }

    /**
     * Get all active parking sessions (currently parked vehicles)
     */
    @GetMapping("/active-sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public PageResponse<ParkingTicketDTO> getActiveSessions(
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        return PageResponse.of(parkingEntryExitService.getActiveTickets(
                Pagination.of(page, size, Sort.by("entryTime", "ticketId"))), this::convertToDTO);
    }

    /**
     * Get user's parking history
     */
    @GetMapping("/history/{userId}")
    public PageResponse<ParkingTicketDTO> getUserParkingHistory(@PathVariable Long userId,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        currentUserService.checkAccess(userId);
        return PageResponse.of(parkingEntryExitService.getUserParkingHistory(userId,
                Pagination.of(page, size, Sort.by(Sort.Direction.DESC, "exitTime", "ticketId"))), this::convertToDTO);
    }

    /**
     * Get user's current active parking sessions
     */
    @GetMapping("/user-active/{userId}")
    public ResponseEntity<?> getUserActiveParking(@PathVariable Long userId) {
        currentUserService.checkAccess(userId);
        List<ParkingTicket> activeParking = parkingEntryExitService.getUserActiveParking(userId);
        List<ParkingTicketDTO> dtos = activeParking.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("count", activeParking.size());
        response.put("activeParking", dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * Get current occupancy (public)
     */
    @GetMapping("/occupancy")
    public ResponseEntity<?> getCurrentOccupancy() {
        Long occupancy = parkingEntryExitService.getCurrentOccupancy();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("currentOccupancy", occupancy);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel parking ticket
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelTicket(@RequestParam String ticketReference,
                                           @RequestParam(required = false, defaultValue = "No reason provided") String reason) {
        currentUserService.checkAccess(findTicket(ticketReference).getUser().getUserId());
        ParkingTicket ticket = parkingEntryExitService.cancelTicket(ticketReference, reason);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Ticket cancelled successfully");
        response.put("ticket", convertToDTO(ticket));
        return ResponseEntity.ok(response);
    }

    private ParkingTicket findTicket(String ticketReference) {
        return parkingEntryExitService.getTicketDetails(ticketReference)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with reference: " + ticketReference));
    }

    /**
     * Helper method to convert ParkingTicket to DTO
     */
    private ParkingTicketDTO convertToDTO(ParkingTicket ticket) {
        return ParkingTicketDTO.builder()
                .ticketId(ticket.getTicketId())
                .ticketReference(ticket.getTicketReference())
                .userId(ticket.getUser().getUserId())
                .vehicleId(ticket.getVehicle().getVehicleId())
                .parkingSpaceId(ticket.getParkingSpace().getParkingSpaceId())
                .entryTime(ticket.getEntryTime())
                .exitTime(ticket.getExitTime())
                .durationMinutes(ticket.getDurationMinutes())
                .status(ticket.getStatus())
                .notes(ticket.getNotes())
                .build();
    }
}
