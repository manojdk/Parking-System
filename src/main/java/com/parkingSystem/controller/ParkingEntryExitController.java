package com.parkingSystem.controller;

import com.parkingSystem.service.IParkingEntryExitService;
import com.parkingSystem.model.ParkingTicket;
import com.parkingSystem.dto.VehicleEntryRequest;
import com.parkingSystem.dto.ParkingTicketDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * Record vehicle entry
     */
    @PostMapping("/entry")
    public ResponseEntity<?> recordVehicleEntry(@RequestBody VehicleEntryRequest request) {
        try {
            log.info("Vehicle entry request received");
            ParkingTicket ticket = parkingEntryExitService.recordVehicleEntry(
                    request.getUserId(),
                    request.getVehicleId(),
                    request.getParkingSpaceId()
            );

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Vehicle entry recorded successfully");
            response.put("ticket", convertToDTO(ticket));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error recording vehicle entry: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Record vehicle exit
     */
    @PostMapping("/exit")
    public ResponseEntity<?> recordVehicleExit(@RequestParam String ticketReference) {
        try {
            log.info("Vehicle exit request received for ticket: {}", ticketReference);
            ParkingTicket ticket = parkingEntryExitService.recordVehicleExit(ticketReference);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Vehicle exit recorded successfully");
            response.put("ticket", convertToDTO(ticket));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error recording vehicle exit: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get ticket details
     */
    @GetMapping("/ticket/{ticketReference}")
    public ResponseEntity<?> getTicketDetails(@PathVariable String ticketReference) {
        try {
            ParkingTicket ticket = parkingEntryExitService.getTicketDetails(ticketReference)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));
            return ResponseEntity.ok(convertToDTO(ticket));
        } catch (Exception e) {
            log.error("Error fetching ticket: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get all active parking sessions (currently parked vehicles)
     */
    @GetMapping("/active-sessions")
    public ResponseEntity<?> getActiveSessions() {
        try {
            List<ParkingTicket> activeTickets = parkingEntryExitService.getActiveTickets();
            List<ParkingTicketDTO> dtos = activeTickets.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", activeTickets.size());
            response.put("activeParking", dtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching active sessions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get user's parking history
     */
    @GetMapping("/history/{userId}")
    public ResponseEntity<?> getUserParkingHistory(@PathVariable Long userId) {
        try {
            List<ParkingTicket> history = parkingEntryExitService.getUserParkingHistory(userId);
            List<ParkingTicketDTO> dtos = history.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", history.size());
            response.put("parkingHistory", dtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching parking history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get user's current active parking sessions
     */
    @GetMapping("/user-active/{userId}")
    public ResponseEntity<?> getUserActiveParking(@PathVariable Long userId) {
        try {
            List<ParkingTicket> activeParking = parkingEntryExitService.getUserActiveParking(userId);
            List<ParkingTicketDTO> dtos = activeParking.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", activeParking.size());
            response.put("activeParking", dtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching active parking: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get current occupancy
     */
    @GetMapping("/occupancy")
    public ResponseEntity<?> getCurrentOccupancy() {
        try {
            Long occupancy = parkingEntryExitService.getCurrentOccupancy();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("currentOccupancy", occupancy);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching occupancy: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Cancel parking ticket
     */
    @PostMapping("/cancel")
    public ResponseEntity<?> cancelTicket(@RequestParam String ticketReference,
                                           @RequestParam(required = false, defaultValue = "No reason provided") String reason) {
        try {
            ParkingTicket ticket = parkingEntryExitService.cancelTicket(ticketReference, reason);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Ticket cancelled successfully");
            response.put("ticket", convertToDTO(ticket));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error cancelling ticket: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
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
