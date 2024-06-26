package com.parkingSystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "parking_spaces")
@Data
public class ParkingSpace {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "parking_space_id")
	private Long parkingSpaceId;

	@Column(name = "availability_status", nullable = false)
	private String availabilityStatus;

	@Column(name = "location", nullable = false)
	private String location;

	@Column(name = "type", nullable = false)
	private String type;

	@Column(name = "rate", nullable = false)
	private Double rate;

}
