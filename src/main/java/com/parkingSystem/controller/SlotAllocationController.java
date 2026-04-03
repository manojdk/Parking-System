package com.parkingSystem.controller;

import com.parkingSystem.enums.SlotType;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.service.ISlotAllocationService;
import com.parkingSystem.service.impl.SlotAllocationServiceImpl;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for Smart Slot Allocation
 */
@RestController
@RequestMapping("/api/slots")
@AllArgsConstructor
@Slf4j
public class SlotAllocationController {

    private ISlotAllocationService slotAllocationService;

    /**
     * Find nearest available slot by location and type
     */
    @GetMapping("/find-nearest")
    public ResponseEntity<?> findNearestSlot(@RequestParam String location, 
                                              @RequestParam SlotType slotType) {
        try {
            log.info("Finding nearest slot at location: {}, type: {}", location, slotType);
            ParkingSpace slot = slotAllocationService.findNearestAvailableSlot(location, slotType)
                    .orElse(null);

            if (slot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "notfound", "message", "No available slots at this location"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Nearest slot found");
            response.put("slot", slot);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error finding nearest slot: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get available slots by type
     */
    @GetMapping("/available-by-type/{slotType}")
    public ResponseEntity<?> getAvailableSlotsByType(@PathVariable SlotType slotType) {
        try {
            log.info("Fetching available slots by type: {}", slotType);
            List<ParkingSpace> slots = slotAllocationService.findAvailableSlotsByType(slotType);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("slotType", slotType);
            response.put("count", slots.size());
            response.put("slots", slots);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching slots by type: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get available slots by location
     */
    @GetMapping("/available-by-location")
    public ResponseEntity<?> getAvailableSlotsByLocation(@RequestParam String location) {
        try {
            log.info("Fetching available slots at location: {}", location);
            List<ParkingSpace> slots = slotAllocationService.findAvailableSlotsByLocation(location);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("location", location);
            response.put("count", slots.size());
            response.put("slots", slots);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching slots by location: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get all available slots
     */
    @GetMapping("/available-all")
    public ResponseEntity<?> getAllAvailableSlots() {
        try {
            log.info("Fetching all available slots");
            List<ParkingSpace> slots = slotAllocationService.getAllAvailableSlots();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalAvailable", slots.size());
            response.put("slots", slots);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching available slots: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get availability summary (real-time dashboard data)
     */
    @GetMapping("/availability-summary")
    public ResponseEntity<?> getAvailabilitySummary() {
        try {
            log.info("Fetching availability summary");
            SlotAllocationServiceImpl.AvailabilitySummary summary = slotAllocationService.getAvailabilitySummary();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("summary", summary);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching availability summary: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Recommend slot based on vehicle type and location
     */
    @GetMapping("/recommend")
    public ResponseEntity<?> recommendSlot(@RequestParam String location, 
                                            @RequestParam String vehicleType) {
        try {
            log.info("Recommending slot for vehicle type: {} at location: {}", vehicleType, location);
            ParkingSpace recommendedSlot = slotAllocationService.recommendSlot(location, vehicleType)
                    .orElse(null);

            if (recommendedSlot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "notfound", "message", "No slots available for recommendation"));
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Slot recommended");
            response.put("slot", recommendedSlot);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error recommending slot: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Check if specific slot is available
     */
    @GetMapping("/check-availability/{parkingSpaceId}")
    public ResponseEntity<?> checkSlotAvailability(@PathVariable Long parkingSpaceId) {
        try {
            boolean isAvailable = slotAllocationService.isSlotAvailable(parkingSpaceId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("parkingSpaceId", parkingSpaceId);
            response.put("isAvailable", isAvailable);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error checking slot availability: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }
}

