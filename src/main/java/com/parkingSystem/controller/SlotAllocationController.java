package com.parkingSystem.controller;

import com.parkingSystem.controller.support.Pagination;
import com.parkingSystem.dto.AvailabilitySummaryDTO;
import com.parkingSystem.dto.PageResponse;
import com.parkingSystem.dto.ParkingSpaceDTO;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.VehicleType;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.service.ISlotAllocationService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Slot lookups for drivers. All endpoints are public and read-only.
 */
@RestController
@RequestMapping("/api/slots")
@AllArgsConstructor
public class SlotAllocationController {

    /** Lowest floor first, then space number; the id makes the order total for stable pages. */
    static final Sort BY_FLOOR_AND_NUMBER = Sort.by("floorNumber", "spaceNumber", "parkingSpaceId");

    private ISlotAllocationService slotAllocationService;

    /**
     * Find nearest available slot by location and type
     */
    @GetMapping("/find-nearest")
    public ResponseEntity<ParkingSpaceDTO> findNearestSlot(@RequestParam String location,
                                                           @RequestParam SlotType slotType) {
        return slotAllocationService.findNearestAvailableSlot(location, slotType)
                .map(ParkingSpaceDTO::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No available " + slotType + " slots at " + location));
    }

    @GetMapping("/available-by-type/{slotType}")
    public PageResponse<ParkingSpaceDTO> getAvailableSlotsByType(@PathVariable SlotType slotType,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        return PageResponse.of(slotAllocationService.findAvailableSlotsByType(slotType,
                Pagination.of(page, size, BY_FLOOR_AND_NUMBER)), ParkingSpaceDTO::from);
    }

    @GetMapping("/available-by-location")
    public PageResponse<ParkingSpaceDTO> getAvailableSlotsByLocation(@RequestParam String location,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        return PageResponse.of(slotAllocationService.findAvailableSlotsByLocation(location,
                Pagination.of(page, size, BY_FLOOR_AND_NUMBER)), ParkingSpaceDTO::from);
    }

    @GetMapping("/available-all")
    public PageResponse<ParkingSpaceDTO> getAllAvailableSlots(
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        return PageResponse.of(slotAllocationService.getAllAvailableSlots(Pagination.of(page, size, BY_FLOOR_AND_NUMBER)),
                ParkingSpaceDTO::from);
    }

    /**
     * Free spaces per slot type (real-time dashboard data)
     */
    @GetMapping("/availability-summary")
    public ResponseEntity<Map<String, Object>> getAvailabilitySummary() {
        AvailabilitySummaryDTO summary = slotAllocationService.getAvailabilitySummary();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("summary", summary);
        return ResponseEntity.ok(response);
    }

    /**
     * Nearest free slot that suits the vehicle type (CAR, BIKE or EV)
     */
    @GetMapping("/recommend")
    public ResponseEntity<ParkingSpaceDTO> recommendSlot(@RequestParam String location,
                                                         @RequestParam String vehicleType) {
        VehicleType type = VehicleType.fromValue(vehicleType);
        return slotAllocationService.recommendSlot(location, type)
                .map(ParkingSpaceDTO::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("No suitable slot for a " + type + " at " + location));
    }

    /**
     * Check if specific slot is available
     */
    @GetMapping("/check-availability/{parkingSpaceId}")
    public ResponseEntity<Map<String, Object>> checkSlotAvailability(@PathVariable Long parkingSpaceId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "success");
        response.put("parkingSpaceId", parkingSpaceId);
        response.put("isAvailable", slotAllocationService.isSlotAvailable(parkingSpaceId));
        return ResponseEntity.ok(response);
    }
}
