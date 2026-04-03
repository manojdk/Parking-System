package com.parkingSystem.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.parkingSystem.enums.SlotType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "parking_spaces")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpace {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "parking_space_id")
	private Long parkingSpaceId;

	@Column(name = "space_number", unique = true, nullable = false)
	private String spaceNumber;

	@Column(name = "availability_status", nullable = false)
	private String availabilityStatus;

	@Column(name = "location", nullable = false)
	private String location;

	@Column(name = "floor_number")
	private Integer floorNumber;

	@Enumerated(EnumType.STRING)
	@Column(name = "slot_type", nullable = false)
	private SlotType slotType;

	@Column(name = "rate_per_hour", nullable = false)
	private BigDecimal ratePerHour;

	@Column(name = "notes")
	private String notes;

	@Column(name = "is_active", nullable = false)
	private Boolean isActive = true;

	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		this.createdAt = LocalDateTime.now();
		this.updatedAt = LocalDateTime.now();
	}

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = LocalDateTime.now();
	}
}
