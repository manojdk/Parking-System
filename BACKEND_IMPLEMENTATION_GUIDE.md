# Backend Implementation Guide

**Version:** 1.0  
**Last Updated:** June 7, 2026  
**Target Audience:** Backend developers, tech leads  

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [Development Environment Setup](#development-environment-setup)
3. [Code Organization Principles](#code-organization-principles)
4. [Coding Standards](#coding-standards)
5. [Database Guidelines](#database-guidelines)
6. [Service Layer Implementation](#service-layer-implementation)
7. [Controller Development](#controller-development)
8. [Error Handling](#error-handling)
9. [Testing Practices](#testing-practices)
10. [Performance Optimization](#performance-optimization)
11. [Security Best Practices](#security-best-practices)
12. [Deployment Checklist](#deployment-checklist)

---

## Project Structure

### Recommended Directory Layout

```
parking-system/
├── src/main/java/com/parkingSystem/
│   ├── ParkingSystemApplication.java       # Main Spring Boot entry point
│   │
│   ├── config/                             # Configuration classes
│   │   ├── SecurityConfig.java            # Spring Security configuration
│   │   ├── JwtAuthenticationFilter.java    # JWT authentication filter
│   │   ├── JwtUtil.java                   # JWT token utilities
│   │   └── CorsConfig.java                # CORS configuration
│   │
│   ├── controller/                         # REST Controllers
│   │   ├── AuthController.java            # Authentication endpoints
│   │   ├── UserController.java            # User management endpoints
│   │   ├── VehicleController.java         # Vehicle management endpoints
│   │   ├── ParkingSpaceController.java    # Parking space endpoints
│   │   ├── ParkingEntryExitController.java# Entry/Exit endpoints
│   │   ├── ReservationController.java     # Reservation endpoints
│   │   ├── BillingController.java         # Billing endpoints
│   │   ├── SlotAllocationController.java  # Smart allocation endpoints
│   │   └── FeedbackController.java        # Feedback endpoints
│   │
│   ├── service/                            # Business logic layer
│   │   ├── IUserService.java              # User service interface
│   │   ├── IVehicleService.java           # Vehicle service interface
│   │   ├── IParkingSpaceService.java      # Parking space interface
│   │   ├── IParkingEntryExitService.java  # Entry/Exit interface
│   │   ├── IReservationService.java       # Reservation interface
│   │   ├── IBillingService.java           # Billing interface
│   │   ├── ISlotAllocationService.java    # Slot allocation interface
│   │   ├── IFeedbackService.java          # Feedback interface
│   │   ├── ICustomUserDetailsService.java # Custom auth details
│   │   │
│   │   └── impl/                          # Implementation classes
│   │       ├── UserServiceImpl.java
│   │       ├── VehicleServiceImpl.java
│   │       ├── ParkingSpaceServiceImpl.java
│   │       ├── ParkingEntryExitServiceImpl.java
│   │       ├── ReservationServiceImpl.java
│   │       ├── BillingServiceImpl.java
│   │       ├── SlotAllocationServiceImpl.java
│   │       ├── FeedbackServiceImpl.java
│   │       └── CustomUserDetailsService.java
│   │
│   ├── model/                              # JPA Entity classes
│   │   ├── User.java
│   │   ├── Vehicle.java
│   │   ├── ParkingSpace.java
│   │   ├── ParkingTicket.java
│   │   ├── Bill.java
│   │   ├── Reservation.java
│   │   ├── Feedback.java
│   │   └── SlotAllocation.java            # Optional
│   │
│   ├── repository/                         # JPA Repositories
│   │   ├── UserRepository.java
│   │   ├── VehicleRepository.java
│   │   ├── ParkingSpaceRepository.java
│   │   ├── ParkingTicketRepository.java
│   │   ├── BillRepository.java
│   │   ├── ReservationRepository.java
│   │   ├── FeedbackRepository.java
│   │   └── SlotAllocationRepository.java
│   │
│   ├── dto/                                # Data Transfer Objects
│   │   ├── AuthResponse.java
│   │   ├── LoginRequest.java
│   │   ├── UserRegistrationDTO.java
│   │   ├── VehicleDTO.java
│   │   ├── ParkingSpaceDTO.java
│   │   ├── ParkingTicketDTO.java
│   │   ├── BillDTO.java
│   │   ├── ReservationDTO.java
│   │   ├── FeedbackDTO.java
│   │   ├── PaginatedResponse.java
│   │   ├── ErrorResponse.java
│   │   └── SlotAvailabilitySummaryDTO.java
│   │
│   ├── enums/                              # Enumeration classes
│   │   ├── VehicleType.java              # CAR, BIKE, EV
│   │   ├── SlotType.java                 # NORMAL, VIP, HANDICAPPED
│   │   ├── TicketStatus.java             # ACTIVE, CLOSED, CANCELLED
│   │   └── BillingStatus.java            # PENDING, PAID, CANCELLED
│   │
│   ├── gobalExceptionHandler/             # Exception handling
│   │   ├── GlobalExceptionHandler.java   # Central exception handler
│   │   ├── custom exceptions/
│   │   │   ├── NoAvailableSpaceException.java
│   │   │   ├── InvalidCredentialsException.java
│   │   │   ├── DuplicateResourceException.java
│   │   │   ├── ResourceNotFoundException.java
│   │   │   └── UnauthorizedException.java
│   │
│   ├── utils/                             # Utility classes
│   │   ├── ValidationUtil.java           # Validation helpers
│   │   ├── ConversionUtil.java           # DTO-Entity conversion
│   │   ├── DateTimeUtil.java             # Date/time utilities
│   │   └── EncryptionUtil.java           # Encryption helpers
│   │
│   └── uuidTest/                          # Testing helpers (remove in prod)
│
├── src/main/resources/
│   ├── application.properties             # Configuration
│   ├── application-dev.properties         # Dev-specific config
│   ├── application-prod.properties        # Prod-specific config
│   ├── messages.properties                # Internationalization (i18n)
│   └── logback.xml                        # Logging configuration
│
├── src/test/java/com/parkingSystem/
│   ├── ParkingSystemApplicationTests.java
│   ├── service/
│   │   ├── UserServiceTest.java
│   │   ├── VehicleServiceTest.java
│   │   └── ParkingEntryExitServiceTest.java
│   └── controller/
│       ├── AuthControllerTest.java
│       ├── UserControllerTest.java
│       └── ParkingEntryExitControllerTest.java
│
├── pom.xml                                # Maven dependencies
├── README.md                              # Project overview
├── ARCHITECTURE_DESIGN.md                 # HLD document
├── API_DOCUMENTATION_DETAILED.md          # API reference
├── DATABASE_DESIGN.md                     # Database schema details
├── DATABASE_MIGRATION.sql                 # DB migration script
└── DEPLOYMENT_GUIDE.md                    # Deployment instructions
```

---

## Development Environment Setup

### Prerequisites

```
Java Development Kit (JDK): 17 (LTS)
Maven: 3.8+
PostgreSQL: 12+
Git: 2.30+
IDE: IntelliJ IDEA / Eclipse / VS Code
```

### Local Setup Steps

#### 1. Install Dependencies

```bash
# Windows - Using Chocolatey
choco install openjdk17 maven postgresql git

# macOS - Using Homebrew
brew install openjdk@17 maven postgresql git

# Linux - Ubuntu/Debian
sudo apt-get install openjdk-17-jdk maven postgresql git
```

#### 2. Clone and Configure

```bash
# Clone repository
git clone <repository-url>
cd parking-system

# Create local database
psql -U postgres
CREATE DATABASE parking_system_db;
CREATE USER parking_admin WITH PASSWORD 'secure_password';
GRANT ALL PRIVILEGES ON DATABASE parking_system_db TO parking_admin;
\q
```

#### 3. Configure Application Properties

```properties
# src/main/resources/application-dev.properties
spring.application.name=Parking-System
spring.datasource.url=jdbc:postgresql://localhost:5432/parking_system_db
spring.datasource.username=parking_admin
spring.datasource.password=secure_password
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.use_sql_comments=true

# JWT Configuration
jwt.secret=your-very-secure-secret-key-min-32-characters
jwt.expiration=86400000  # 24 hours in milliseconds

# Logging
logging.level.root=INFO
logging.level.com.parkingSystem=DEBUG

# CORS
cors.allowed-origins=http://localhost:3000,http://localhost:4200
cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
cors.allowed-headers=*
cors.allow-credentials=true
```

#### 4. Build and Run

```bash
# Build project
mvn clean install -DskipTests

# Run migrations
psql -U parking_admin -d parking_system_db -f DATABASE_MIGRATION.sql

# Run application
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

#### 5. Verify Setup

```bash
# Check if server is running
curl http://localhost:8080/api/auth/login

# View health status
curl http://localhost:8080/actuator/health
```

---

## Code Organization Principles

### 1. Separation of Concerns

**Principle:** Each layer has a distinct responsibility.

```java
// ❌ BAD: Mixed business logic with HTTP handling
@RestController
public class BadController {
    @PostMapping("/entry")
    public ResponseEntity<?> entry(ParkingEntryRequest req) {
        ParkingTicket ticket = new ParkingTicket();
        ticket.setEntryTime(LocalDateTime.now());
        
        // Database access mixed with controller
        ParkingSpace space = em.find(ParkingSpace.class, req.getSpaceId());
        space.setAvailabilityStatus("occupied");
        em.persist(space);
        
        return ResponseEntity.ok(ticket);
    }
}

// ✓ GOOD: Clean separation
@RestController
@RequestMapping("/api/parking-entry")
public class ParkingEntryExitController {
    private final IParkingEntryExitService service;
    
    @PostMapping("/record-entry")
    public ResponseEntity<ParkingTicketDTO> recordEntry(
            @RequestBody @Valid ParkingEntryRequest request) {
        ParkingTicket ticket = service.recordEntry(request);
        return ResponseEntity.status(201).body(convertToDTO(ticket));
    }
}

// Service handles business logic
@Service
@Transactional
public class ParkingEntryExitServiceImpl implements IParkingEntryExitService {
    private final ParkingSpaceRepository spaceRepo;
    private final ParkingTicketRepository ticketRepo;
    
    @Override
    public ParkingTicket recordEntry(ParkingEntryRequest request) 
            throws NoAvailableSpaceException {
        // Business logic
        ParkingSpace space = spaceRepo.findById(request.getSpaceId())
            .orElseThrow(() -> new ResourceNotFoundException("Space not found"));
        
        if (!"available".equals(space.getAvailabilityStatus())) {
            throw new NoAvailableSpaceException("Space is occupied");
        }
        
        ParkingTicket ticket = ParkingTicket.builder()
            .parkingSpace(space)
            .entryTime(LocalDateTime.now())
            .status(TicketStatus.ACTIVE)
            .build();
        
        ticketRepo.save(ticket);
        space.setAvailabilityStatus("occupied");
        spaceRepo.save(space);
        
        return ticket;
    }
}

// Repository handles data access
@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {
    List<ParkingSpace> findByAvailabilityStatus(String status);
    List<ParkingSpace> findBySlotTypeAndAvailabilityStatus(SlotType type, String status);
}
```

### 2. Dependency Injection

**Use constructor injection for immutability:**

```java
// ❌ BAD: Field injection, mutable dependencies
@Service
public class BadService {
    @Autowired
    private UserRepository userRepo;  // Can be null at runtime
    
    public void process() {
        if (userRepo == null) {  // Defensive null check needed
            throw new RuntimeException("UserRepo not injected");
        }
    }
}

// ✓ GOOD: Constructor injection
@Service
@RequiredArgsConstructor  // Lombok generates constructor
public class GoodService {
    private final UserRepository userRepo;  // Immutable, never null
    private final IVehicleService vehicleService;
    
    public void process() {
        // Direct usage, no null checks needed
        User user = userRepo.findById(1L).orElseThrow();
    }
}
```

### 3. DTO Pattern

**Always use DTOs for API contracts:**

```java
// ❌ BAD: Exposing entity directly
@GetMapping("/users/{id}")
public ResponseEntity<User> getUser(@PathVariable Long id) {
    User user = userRepo.findById(id).orElseThrow();
    return ResponseEntity.ok(user);  // Exposes internal fields, lazy-loaded associations
}

// ✓ GOOD: Using DTO
@GetMapping("/users/{id}")
public ResponseEntity<UserDTO> getUser(@PathVariable Long id) {
    User user = userRepo.findById(id).orElseThrow();
    UserDTO dto = UserMapper.toDTO(user);  // Controlled exposure
    return ResponseEntity.ok(dto);
}

// DTO Definition
@Data
@Builder
public class UserDTO {
    private Long userId;
    private String userName;
    private String userEmail;
    private String role;
    // ✓ Password NOT exposed
    // ✓ Unnecessary fields excluded
}

// Mapper (Utility method)
public class UserMapper {
    public static UserDTO toDTO(User user) {
        return UserDTO.builder()
            .userId(user.getUserId())
            .userName(user.getUserName())
            .userEmail(user.getUserEmail())
            .role(user.getRole())
            .build();
    }
    
    public static User toEntity(UserDTO dto) {
        return User.builder()
            .userName(dto.getUserName())
            .userEmail(dto.getUserEmail())
            .role(dto.getRole())
            .build();
    }
}
```

---

## Coding Standards

### 1. Naming Conventions

```java
// Classes: PascalCase
public class UserService {}
public class ParkingEntryExitController {}

// Methods: camelCase
public void recordParkingEntry() {}
public List<User> getAllActiveUsers() {}

// Constants: UPPER_SNAKE_CASE
private static final int MAX_PARKING_DURATION = 24;
private static final String ERROR_SPACE_NOT_FOUND = "Space not found";

// Variables: camelCase
String licencePlate = "KA-01-AB-1234";
int parkingDurationMinutes = 120;

// Interfaces: I + PascalCase
public interface IUserService {}
public interface IParkingSpaceRepository {}

// Boolean variables: is/has prefix
private boolean isActive = true;
private boolean hasParked = false;
```

### 2. Code Structure Standards

#### Service Interface

```java
/**
 * Service interface for parking entry and exit operations.
 * Handles the business logic for recording vehicle entry and exit.
 * 
 * @author Development Team
 * @version 1.0
 * @since 2026-06-07
 */
@Service
public interface IParkingEntryExitService {
    
    /**
     * Records a vehicle's entry into the parking system.
     * 
     * @param request The entry request containing user, vehicle, and space details
     * @return The created parking ticket with reference number
     * @throws NoAvailableSpaceException if space is not available
     * @throws ResourceNotFoundException if vehicle or space not found
     */
    ParkingTicket recordEntry(ParkingEntryRequest request) 
        throws NoAvailableSpaceException, ResourceNotFoundException;
    
    /**
     * Records a vehicle's exit from the parking system.
     * Calculates duration, updates ticket status, and triggers billing.
     * 
     * @param ticketId The parking ticket ID to close
     * @return The closed parking ticket with exit details and generated bill
     * @throws ResourceNotFoundException if ticket not found
     * @throws InvalidOperationException if ticket is already closed
     */
    ParkingTicket recordExit(Long ticketId) 
        throws ResourceNotFoundException, InvalidOperationException;
}
```

#### Service Implementation

```java
@Service
@Slf4j                                    // Lombok logger
@Transactional(readOnly = false)         // Transaction management
@RequiredArgsConstructor                 // Constructor injection
public class ParkingEntryExitServiceImpl implements IParkingEntryExitService {
    
    private final ParkingTicketRepository ticketRepo;
    private final ParkingSpaceRepository spaceRepo;
    private final VehicleRepository vehicleRepo;
    private final IBillingService billingService;
    
    @Override
    @Transactional(rollbackOn = Exception.class)
    public ParkingTicket recordEntry(ParkingEntryRequest request) {
        log.info("Recording parking entry for user: {}, vehicle: {}, space: {}",
            request.getUserId(), request.getVehicleId(), request.getParkingSpaceId());
        
        // Validation
        if (request.getUserId() == null || request.getVehicleId() == null) {
            log.warn("Invalid entry request - missing required fields");
            throw new IllegalArgumentException("User ID and Vehicle ID are required");
        }
        
        // Fetch related entities
        Vehicle vehicle = vehicleRepo.findById(request.getVehicleId())
            .orElseThrow(() -> {
                log.error("Vehicle not found: {}", request.getVehicleId());
                return new ResourceNotFoundException("Vehicle not found");
            });
        
        ParkingSpace space = spaceRepo.findById(request.getParkingSpaceId())
            .orElseThrow(() -> {
                log.error("Parking space not found: {}", request.getParkingSpaceId());
                return new ResourceNotFoundException("Parking space not found");
            });
        
        // Business logic validation
        if (!"available".equals(space.getAvailabilityStatus())) {
            log.warn("Space {} is not available. Current status: {}",
                request.getParkingSpaceId(), space.getAvailabilityStatus());
            throw new NoAvailableSpaceException("Parking space is not available");
        }
        
        // Create ticket
        ParkingTicket ticket = ParkingTicket.builder()
            .user(new User()) {{setUserId(request.getUserId());}}
            .vehicle(vehicle)
            .parkingSpace(space)
            .entryTime(LocalDateTime.now())
            .entryPhotoUrl(request.getEntryPhotoUrl())
            .status(TicketStatus.ACTIVE)
            .build();
        
        // Save ticket
        ParkingTicket savedTicket = ticketRepo.save(ticket);
        
        // Update space availability
        space.setAvailabilityStatus("occupied");
        spaceRepo.save(space);
        
        log.info("Parking ticket created successfully: {} for space: {}",
            savedTicket.getTicketReference(), space.getSpaceNumber());
        
        return savedTicket;
    }
    
    @Override
    @Transactional(rollbackOn = Exception.class)
    public ParkingTicket recordExit(Long ticketId) {
        log.info("Recording parking exit for ticket: {}", ticketId);
        
        // Fetch active ticket
        ParkingTicket ticket = ticketRepo.findById(ticketId)
            .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));
        
        // Validate ticket status
        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            log.warn("Ticket {} is already {}", ticketId, ticket.getStatus());
            throw new InvalidOperationException("Ticket is not active");
        }
        
        // Calculate duration
        LocalDateTime exitTime = LocalDateTime.now();
        long durationMinutes = ChronoUnit.MINUTES.between(ticket.getEntryTime(), exitTime);
        
        // Update ticket
        ticket.setExitTime(exitTime);
        ticket.setDurationMinutes(durationMinutes);
        ticket.setStatus(TicketStatus.CLOSED);
        
        ParkingTicket updatedTicket = ticketRepo.save(ticket);
        
        // Free up space
        ParkingSpace space = ticket.getParkingSpace();
        space.setAvailabilityStatus("available");
        spaceRepo.save(space);
        
        // Generate billing
        billingService.generateBill(updatedTicket);
        
        log.info("Parking exit recorded successfully. Duration: {} minutes", durationMinutes);
        
        return updatedTicket;
    }
}
```

#### Controller

```java
/**
 * REST Controller for parking entry and exit operations.
 * All endpoints require JWT authentication via Bearer token.
 * 
 * @author Development Team
 * @version 1.0
 */
@RestController
@RequestMapping("/api/parking-entry")
@Slf4j
@RequiredArgsConstructor
@CrossOrigin(origins = "${cors.allowed-origins}")
@Validated
public class ParkingEntryExitController {
    
    private final IParkingEntryExitService entryExitService;
    
    /**
     * Records vehicle entry into parking.
     * 
     * @param request The parking entry request
     * @return 201 CREATED with ticket details
     * @throws NoAvailableSpaceException if space not available
     */
    @PostMapping("/record-entry")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")  // Security annotation
    public ResponseEntity<ParkingTicketDTO> recordEntry(
            @RequestBody @Valid ParkingEntryRequest request) {
        
        log.debug("Received parking entry request: {}", request);
        
        ParkingTicket ticket = entryExitService.recordEntry(request);
        ParkingTicketDTO response = ParkingTicketMapper.toDTO(ticket);
        
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
    
    /**
     * Records vehicle exit from parking.
     * Calculates fees and generates bill.
     * 
     * @param ticketId ID of parking ticket
     * @return 200 OK with exit details and bill
     */
    @PostMapping("/record-exit")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ExitResponseDTO> recordExit(
            @RequestParam @NotNull Long ticketId) {
        
        log.debug("Received parking exit request for ticket: {}", ticketId);
        
        ParkingTicket ticket = entryExitService.recordExit(ticketId);
        ExitResponseDTO response = ExitResponseMapper.toDTO(ticket);
        
        return ResponseEntity.ok(response);
    }
}
```

### 3. Exception Handling

```java
// Custom Exceptions
@Getter
public class NoAvailableSpaceException extends RuntimeException {
    private final String errorCode = "NO_AVAILABLE_SPACE";
    
    public NoAvailableSpaceException(String message) {
        super(message);
    }
}

public class ResourceNotFoundException extends RuntimeException {
    private final String errorCode = "RESOURCE_NOT_FOUND";
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// Global Exception Handler
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        log.error("Resource not found: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .message(ex.getMessage())
            .errorCode("RESOURCE_NOT_FOUND")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response);
    }
    
    @ExceptionHandler(NoAvailableSpaceException.class)
    public ResponseEntity<ErrorResponse> handleNoAvailableSpace(
            NoAvailableSpaceException ex, HttpServletRequest request) {
        
        log.error("No available space: {}", ex.getMessage());
        
        ErrorResponse response = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message(ex.getMessage())
            .errorCode("NO_AVAILABLE_SPACE")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        String message = ex.getBindingResult().getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        log.error("Validation error: {}", message);
        
        ErrorResponse response = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .message("Validation failed")
            .details(message)
            .errorCode("INVALID_INPUT")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unexpected error", ex);
        
        ErrorResponse response = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .message("Internal server error")
            .errorCode("INTERNAL_ERROR")
            .timestamp(LocalDateTime.now())
            .path(request.getRequestURI())
            .build();
        
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response);
    }
}
```

---

## Database Guidelines

### 1. Query Optimization

```java
// ❌ BAD: N+1 Query Problem (1 query + N queries)
List<User> users = userRepo.findAll();
for (User user : users) {
    List<Vehicle> vehicles = vehicleRepo.findByUserId(user.getUserId());
    // Executes N queries
}

// ✓ GOOD: Join with Fetch
@Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.vehicles WHERE u.isActive = true")
List<User> findAllWithVehicles();

// ✓ GOOD: Pagination
Page<User> users = userRepo.findAll(PageRequest.of(0, 10));

// ❌ BAD: SELECT * (fetches unnecessary fields)
Query query = em.createQuery("SELECT u FROM User u");

// ✓ GOOD: Select specific fields if needed
Query query = em.createQuery(
    "SELECT new UserDTO(u.userId, u.userName) FROM User u");
```

### 2. Index Strategy

```sql
-- Frequently searched fields
CREATE INDEX idx_user_email ON parking_user(user_email);
CREATE INDEX idx_user_username ON parking_user(user_name);
CREATE INDEX idx_vehicle_license_plate ON vehicles(license_plate);

-- Foreign key relationships
CREATE INDEX idx_vehicle_user_id ON vehicles(user_id);
CREATE INDEX idx_ticket_user_id ON parking_tickets(user_id);
CREATE INDEX idx_ticket_vehicle_id ON parking_tickets(vehicle_id);

-- Status/State columns
CREATE INDEX idx_parking_space_availability ON parking_spaces(availability_status);
CREATE INDEX idx_ticket_status ON parking_tickets(status);
CREATE INDEX idx_bill_status ON bills(status);

-- Complex queries
CREATE INDEX idx_bill_user_status ON bills(user_id, status);
CREATE INDEX idx_ticket_status_exit_time ON parking_tickets(status, exit_time);
```

### 3. Transaction Management

```java
// ❌ BAD: Long-running transaction
@Transactional
public void processParkingData() {
    List<ParkingTicket> tickets = ticketRepo.findAll();  // Locks table
    
    // Long processing (seconds)
    for (ParkingTicket ticket : tickets) {
        // Complex business logic
        Thread.sleep(5000);  // Simulated delay
    }
}

// ✓ GOOD: Batch processing with smaller transactions
public void processParkingData() {
    int batchSize = 100;
    int offset = 0;
    
    while (true) {
        List<ParkingTicket> tickets = ticketRepo.findTicketsForProcessing(
            PageRequest.of(offset / batchSize, batchSize)
        ).getContent();
        
        if (tickets.isEmpty()) break;
        
        processBatch(tickets);  // Each batch in separate transaction
        offset += batchSize;
    }
}

@Transactional
private void processBatch(List<ParkingTicket> tickets) {
    for (ParkingTicket ticket : tickets) {
        // Process ticket
    }
    // Transaction commits here
}
```

---

## Service Layer Implementation

### Best Practices

```java
@Service
@Transactional(readOnly = false)
@Slf4j
@RequiredArgsConstructor
public class BillingServiceImpl implements IBillingService {
    
    private final BillRepository billRepo;
    private final ParkingTicketRepository ticketRepo;
    private final ParkingSpaceRepository spaceRepo;
    
    // 1. Use meaningful method names
    @Override
    public Bill generateBill(ParkingTicket parkingTicket) {
        // Implementation
    }
    
    // 2. Keep methods focused (Single Responsibility)
    private BillCalculation calculateBillAmount(ParkingTicket ticket) {
        // Only calculates bill amount
    }
    
    // 3. Validate inputs
    private void validateBillGenerationRequest(ParkingTicket ticket) {
        if (ticket == null) {
            throw new IllegalArgumentException("Ticket cannot be null");
        }
        if (ticket.getExitTime() == null) {
            throw new InvalidOperationException("Ticket has no exit time");
        }
    }
    
    // 4. Use business logic methods
    private BigDecimal applyDiscountPolicy(BigDecimal amount, User user) {
        if (isVIPUser(user)) {
            return amount.multiply(BigDecimal.valueOf(0.9));  // 10% discount
        }
        return amount;
    }
}
```

---

## Controller Development

### Best Practices

```java
@RestController
@RequestMapping("/api/resource")
@Validated
@CrossOrigin(origins = "${cors.allowed-origins}")
@RequiredArgsConstructor
@Slf4j
public class ResourceController {
    
    private final IResourceService service;
    private final ResourceMapper mapper;
    
    // 1. Use appropriate HTTP methods and status codes
    @GetMapping("/{id}")
    public ResponseEntity<ResourceDTO> getResource(@PathVariable Long id) {
        Resource resource = service.getResourceById(id);
        return ResponseEntity.ok(mapper.toDTO(resource));
    }
    
    // 2. Use @Valid for automatic validation
    @PostMapping
    public ResponseEntity<ResourceDTO> createResource(
            @RequestBody @Valid CreateResourceRequest request) {
        Resource resource = service.createResource(request);
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mapper.toDTO(resource));
    }
    
    // 3. Implement pagination
    @GetMapping
    public ResponseEntity<Page<ResourceDTO>> listResources(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sort) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.ASC, sort);
        Page<Resource> resources = service.getAllResources(pageable);
        Page<ResourceDTO> dtos = resources.map(mapper::toDTO);
        
        return ResponseEntity.ok(dtos);
    }
    
    // 4. Use @PreAuthorize for role-based access
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteResource(@PathVariable Long id) {
        service.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
    
    // 5. Add security for sensitive operations
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'ADMIN')")
    public ResponseEntity<ResourceDTO> updateResource(
            @PathVariable Long id,
            @RequestBody @Valid UpdateResourceRequest request,
            @LoadAuthorization String userId) {  // Custom annotation to get current user
        
        verifyOwnership(id, userId);  // Additional security check
        Resource resource = service.updateResource(id, request);
        return ResponseEntity.ok(mapper.toDTO(resource));
    }
}
```

---

## Error Handling

### Best Practices

```java
// Custom exception with meaningful information
public class BillingException extends RuntimeException {
    private final ErrorCode errorCode;
    private final Map<String, Object> context;
    
    public BillingException(ErrorCode errorCode, String message, 
                           Map<String, Object> context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }
}

// Enum for error codes
public enum ErrorCode {
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", "Account has insufficient funds"),
    INVALID_AMOUNT("INVALID_AMOUNT", "Amount must be greater than zero"),
    PROCESSING_FAILED("PROCESSING_FAILED", "Payment processing failed");
    
    private final String code;
    private final String message;
}
```

---

## Testing Practices

### Unit Testing

```java
@SpringBootTest
@Slf4j
class BillingServiceTest {
    
    @Mock
    private BillRepository billRepository;
    
    @InjectMocks
    private BillingServiceImpl billingService;
    
    private ParkingTicket sampleTicket;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        sampleTicket = createSampleTicket();
    }
    
    @Test
    @DisplayName("Should generate bill successfully for completed parking")
    void testGenerateBillSuccess() {
        // Arrange
        Bill expectedBill = createExpectedBill();
        given(billRepository.save(any())).willReturn(expectedBill);
        
        // Act
        Bill result = billingService.generateBill(sampleTicket);
        
        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getTotalAmount()).isGreaterThan(BigDecimal.ZERO);
        verify(billRepository).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception for invalid ticket")
    void testGenerateBillWithInvalidTicket() {
        // Act & Assert
        assertThrows(InvalidOperationException.class, 
            () -> billingService.generateBill(null));
    }
}
```

### Integration Testing

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class ParkingEntryExitIntegrationTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    @DisplayName("Should record entry and exit successfully")
    void testCompleteParking() {
        // Entry
        ParkingEntryRequest entryRequest = createEntryRequest();
        ResponseEntity<ParkingTicketDTO> entryResponse = 
            restTemplate.postForEntity(
                "http://localhost:" + port + "/api/parking-entry/record-entry",
                entryRequest,
                ParkingTicketDTO.class
            );
        
        assertThat(entryResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long ticketId = entryResponse.getBody().getTicketId();
        
        // Exit
        ExitRequest exitRequest = new ExitRequest(ticketId);
        ResponseEntity<ExitResponseDTO> exitResponse = 
            restTemplate.postForEntity(
                "http://localhost:" + port + "/api/parking-entry/record-exit",
                exitRequest,
                ExitResponseDTO.class
            );
        
        assertThat(exitResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exitResponse.getBody().getBillId()).isNotNull();
    }
}
```

---

## Performance Optimization

### Caching

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ParkingSpaceService {
    
    private final ParkingSpaceRepository spaceRepo;
    
    // Cache results for 5 minutes
    @Cacheable(value = "availableSpaces", unless = "#result.isEmpty()")
    public List<ParkingSpace> getAvailableSpaces() {
        log.info("Fetching available spaces from database");
        return spaceRepo.findByAvailabilityStatus("available");
    }
    
    // Invalidate cache when space is updated
    @CacheEvict(value = "availableSpaces", allEntries = true)
    public void updateParkingSpace(ParkingSpace space) {
        spaceRepo.save(space);
    }
}
```

### Database Connection Pooling

```properties
# application.properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000
spring.datasource.hikari.idle-timeout=60000
spring.datasource.hikari.max-lifetime=1800000
```

---

## Security Best Practices

### Password Hashing

```java
@Configuration
public class PasswordEncoderConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // Strength 12
    }
}

// Usage in service
@Service
@RequiredArgsConstructor
public class UserServiceImpl {
    
    private final PasswordEncoder passwordEncoder;
    
    public void registerUser(UserRegistrationDTO request) {
        String hashedPassword = passwordEncoder.encode(request.getPassword());
        // Store hashed password
    }
}
```

### Input Validation

```java
@PostMapping("/register")
public ResponseEntity<UserDTO> register(
        @RequestBody @Valid UserRegistrationDTO request) {
    // Validation annotations
    // - @NotNull
    // - @NotBlank
    // - @Email
    // - @Pattern(regexp = "regex")
    // - @Size(min=8, max=20)
}
```

---

## Deployment Checklist

```markdown
## Pre-Deployment Checklist

### Code Quality
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Code coverage > 80%
- [ ] No SonarQube critical issues
- [ ] No security vulnerabilities (OWASP)

### Performance
- [ ] Database indexes optimized
- [ ] Query plans verified
- [ ] Connection pool configured
- [ ] Response times < 200ms

### Security
- [ ] All passwords hashed (BCrypt)
- [ ] JWT secrets configured
- [ ] CORS properly configured
- [ ] SQL injection prevention (parameterized queries)
- [ ] Rate limiting implemented

### Configuration
- [ ] Environment variables set
- [ ] Database migrations tested
- [ ] Logging configured properly
- [ ] Error messages don't leak sensitive data

### Documentation
- [ ] API documentation updated
- [ ] Code comments for complex logic
- [ ] README updated
- [ ] Deployment guide prepared

### Monitoring
- [ ] Application health checks configured
- [ ] Logging setup verified
- [ ] Metrics collection enabled
- [ ] Alerts configured
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-07 | Initial backend implementation guide |

---

**Document Prepared By:** Development Team  
**Last Updated:** 2026-06-07  
**Next Review:** 2026-09-07

**End of Document**

