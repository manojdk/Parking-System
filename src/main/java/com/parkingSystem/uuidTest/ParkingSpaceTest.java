package com.parkingSystem.uuidTest;

import java.util.UUID;

import org.hibernate.annotations.GenericGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "paring_test_uuid")
@Data
public class ParkingSpaceTest {

	@Id
	@GeneratedValue(generator = "uuid")
	@GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "parking_space_id", columnDefinition = "uuid", updatable = false)
	private UUID parkingSpaceId;

	@Column(name = "availability_status", nullable = false)
	private String availabilityStatus;

	@Column(name = "location", nullable = false)
	private String location;

	@Column(name = "type", nullable = false)
	private String type;

	@Column(name = "rate", nullable = false)
	private Double rate;
}
