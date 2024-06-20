package com.parkingSystem.model;

import java.sql.Date;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Entity
@Data
@Table(name = "parking_user")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Long userId;

	@Column(name = "user_name",unique = true)
	@NotBlank(message = "User Name is madatory")
	private String userName;

	@Column(name = "user_email", unique = true)
	@NotBlank(message = "Email is madatory")
	private String userEmail;
	
	@Column(name = "user_password")
	@NotBlank(message = "Password is madatory")
	private String password;

	@Column(name = "registration_date")
	private Date registrationDate;

	@Column(name = "license_plate")
    @NotBlank(message = "License plate is mandatory")
	private String licencePlate;

	@Column(name = "created_date", nullable = false)
	private LocalDateTime createdDate;

}
