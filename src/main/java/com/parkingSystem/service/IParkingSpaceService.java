package com.parkingSystem.service;

import com.parkingSystem.model.ParkingSpace;
import java.util.List;
import java.util.Map;
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
    List<ParkingSpace> getAllParkingSpaces();

    /**
     * Get Parking space detail by parkingSpaceId (helper class)
     *
     * @param parkingSpaceId Parking space ID
     * @return Parking space data
     */
    Optional<ParkingSpace> getParkingSpaceById(long parkingSpaceId);

    /**
     * Get available Parking space detail
     *
     * @param availabilityStatus Status available or occupied
     * @return Parking space data
     */
    List<ParkingSpace> getAvailableParkingSpaces(String availabilityStatus);

    /**
     * Get Parking Space detail by parkingSpaceId
     *
     * @param parkingSpaceId Parking space ID
     * @return Parking space data
     */
    Map<String, Object> getParkingSpaceDetails(Long parkingSpaceId);

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
     * @param availabilityStatus Availaible space
     * @return Updated Parking space data
     */
    ParkingSpace updateParkingSpace(Long parkingSpaceId, String location, String type, Double rate,
            Boolean availabilityStatus);

    /**
     * Delete Parking space by Parking space Id
     *
     * @param parkingSpaceId Parking space ID
     */
    void deleteParkingSpace(Long parkingSpaceId);
}
