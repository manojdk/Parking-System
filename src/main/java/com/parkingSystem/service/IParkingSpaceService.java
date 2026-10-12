package com.parkingSystem.service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.model.ParkingSpace;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for managing parking spaces
 */
public interface IParkingSpaceService {

    /**
     * Get all Parking Spaces details
     *
     * @return Parking space data
     */
    Page<ParkingSpace> getAllParkingSpaces(Pageable pageable);

    /**
     * Get Parking space detail by parkingSpaceId (helper class)
     *
     * @param parkingSpaceId Parking space ID
     * @return Parking space data
     */
    Optional<ParkingSpace> getParkingSpaceById(long parkingSpaceId);

    /**
     * In-service parking spaces with the given status
     *
     * @param availabilityStatus status to filter on
     * @return Parking space data
     */
    Page<ParkingSpace> getAvailableParkingSpaces(SpaceStatus availabilityStatus, Pageable pageable);

    /**
     * Get Parking Space detail by parkingSpaceId
     *
     * @param parkingSpaceId Parking space ID
     * @return Parking space data
     */
    ParkingSpace getParkingSpaceDetails(Long parkingSpaceId);

    /**
     * Adding Parking space
     *
     * @param parkingSpace {@link ParkingSpace}
     * @return save Parking space data
     */
    ParkingSpace addParkingSpace(ParkingSpace parkingSpace);

    /**
     * Add list of Parking space
     *
     * @param parkingSpaceRequests list of {@link ParkingSpace}
     * @return List of saved Parking space data
     */
    List<ParkingSpace> addMultipleParkingSpaces(List<ParkingSpace> parkingSpaceRequests);

    /**
     * Update Parking Space Data
     *
     * @param parkingSpaceId     Parking space ID
     * @param location           Location of parking space
     * @param type               Type of space
     * @param rate               Rate of parking space
     * @param status             AVAILABLE or OUT_OF_SERVICE; null keeps the current status
     * @return Updated Parking space data
     */
    ParkingSpace updateParkingSpace(Long parkingSpaceId, String location, SlotType type, BigDecimal rate,
            SpaceStatus status);

    /**
     * Remove a parking space. A space with tickets or reservations in its history is deactivated (kept for
     * the records) instead of deleted. A space that is RESERVED or OCCUPIED cannot be removed.
     *
     * @param parkingSpaceId Parking space ID
     */
    void deleteParkingSpace(Long parkingSpaceId);
}
