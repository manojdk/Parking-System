package com.parkingSystem.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.parkingSystem.uuidTest.ParkingSpaceTest;

@Repository
public interface ParkingTestRepo extends JpaRepository<ParkingSpaceTest, UUID> {


}
