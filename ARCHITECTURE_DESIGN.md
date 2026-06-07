# Parking System - Architecture Design Document (HLD)

**Version:** 1.0  
**Last Updated:** June 7, 2026  
**Status:** Production Ready  
**Audience:** Development Team, DevOps, Architects

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [System Overview](#system-overview)
3. [Architecture Patterns](#architecture-patterns)
4. [Component Architecture](#component-architecture)
5. [Data Flow Architecture](#data-flow-architecture)
6. [Database Design](#database-design)
7. [Security Architecture](#security-architecture)
8. [Integration Points](#integration-points)
9. [Deployment Architecture](#deployment-architecture)
10. [Scalability & Performance](#scalability--performance)
11. [Monitoring & Observability](#monitoring--observability)

---

## Executive Summary

**Parking System** is an enterprise-grade backend service built on **Spring Boot 3.3.1** with PostgreSQL database. It provides comprehensive parking management capabilities including space allocation, vehicle tracking, billing, reservations, and user management with JWT-based authentication.

### Key Characteristics
- **Architecture Pattern:** Layered Architecture with Service-Oriented Design
- **Database:** PostgreSQL 12+
- **Data Access:** JPA (Hibernate ORM)
- **Authentication:** JWT (JSON Web Tokens)
- **Java Version:** 17 (LTS)
- **Build Tool:** Maven 3.8+

---

## System Overview

### High-Level Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT LAYER                              │
│              (Web App / Mobile App / Third-party)                │
└────────────────────────┬──────────────────────────────────────────┘
                         │ HTTPS/REST API
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                  PRESENTATION LAYER                              │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Controllers (REST Endpoints) - @RestController          │  │
│  │  ├─ AuthController          (Authentication)             │  │
│  │  ├─ UserController          (User Management)            │  │
│  │  ├─ VehicleController       (Vehicle Management)         │  │
│  │  ├─ ParkingSpaceController  (Parking Space Mgmt)         │  │
│  │  ├─ ParkingEntryExitController (Entry/Exit)             │  │
│  │  ├─ ReservationController   (Reservations)              │  │
│  │  ├─ BillingController       (Billing & Invoicing)       │  │
│  │  ├─ SlotAllocationController (Smart Allocation)         │  │
│  │  └─ FeedbackController      (User Feedback)             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                  │                                                │
│                  ▼                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Filters & Interceptors                                  │  │
│  │  ├─ JwtAuthenticationFilter (JWT Validation)            │  │
│  │  ├─ CorsConfig             (CORS Handling)              │  │
│  │  └─ Exception Handlers     (Centralized Error Mgmt)     │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────┬──────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    BUSINESS LOGIC LAYER                          │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Service Layer (Interfaces)                              │  │
│  │  ├─ IUserService            (User Operations)            │  │
│  │  ├─ IVehicleService         (Vehicle Operations)         │  │
│  │  ├─ IParkingSpaceService    (Space Operations)           │  │
│  │  ├─ IParkingEntryExitService(Entry/Exit Logic)          │  │
│  │  ├─ IReservationService     (Reservation Logic)         │  │
│  │  ├─ IBillingService         (Billing Calculations)      │  │
│  │  ├─ ISlotAllocationService  (Smart Allocation)          │  │
│  │  ├─ IFeedbackService        (Feedback Mgmt)             │  │
│  │  └─ ICustomUserDetailsService(Auth Details)             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                  │                                                │
│                  ▼                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Service Implementations (impl/*)                        │  │
│  │  └─ Concrete business logic execution                    │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────┬──────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                      DATA ACCESS LAYER                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Repositories (JPA Repositories)                         │  │
│  │  ├─ UserRepository                                       │  │
│  │  ├─ VehicleRepository                                    │  │
│  │  ├─ ParkingSpaceRepository                               │  │
│  │  ├─ ParkingTicketRepository                              │  │
│  │  ├─ BillRepository                                       │  │
│  │  ├─ ReservationRepository                                │  │
│  │  ├─ FeedbackRepository                                   │  │
│  │  └─ SlotAllocationRepository                             │  │
│  └──────────────────────────────────────────────────────────┘  │
│                  │                                                │
│                  ▼                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  JPA/Hibernate ORM Mapping                               │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────┬──────────────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                      PERSISTENCE LAYER                           │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  PostgreSQL Database (12+)                               │  │
│  │  ├─ Tables (Users, Vehicles, Spaces, Tickets, etc.)    │  │
│  │  ├─ Indexes (Performance Optimization)                  │  │
│  │  ├─ Views (parking_dashboard, revenue_report, etc.)    │  │
│  │  ├─ Stored Procedures (Complex Operations)             │  │
│  │  └─ Constraints (Data Integrity)                        │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Architecture Patterns

### 1. **Layered Architecture**
The system follows a classic 4-tier layered architecture:

```
Presentation Layer   → Handles HTTP requests/responses
Business Logic Layer → Implements business rules
Data Access Layer    → Database operations
Persistence Layer    → Physical database
```

**Benefits:**
- Clear separation of concerns
- Easy to test and maintain
- Loose coupling between layers
- Easy onboarding for new developers

### 2. **Service-Oriented Design**
- All business logic encapsulated in service interfaces
- Consistent implementation patterns
- Easy to mock and unit test

### 3. **Repository Pattern**
- Spring Data JPA repositories for data access
- Custom query methods for complex operations
- Automatic implementation generation

### 4. **DTO (Data Transfer Object) Pattern**
- Separation between entity and API contracts
- Request/Response validation
- Flexible API schemas independent of database

### 5. **Dependency Injection**
- Spring IoC Container for object management
- Constructor injection for immutability
- Easy testing with mock dependencies

---

## Component Architecture

### Core Components

#### 1. **Authentication & Security Component**

```
Authentication Flow:
┌──────────────┐         ┌──────────────────┐
│  Client      │────────→│  AuthController  │
│              │         └─────────┬────────┘
│              │                   │
│              │                   ▼
│              │         ┌──────────────────────┐
│              │         │ AuthenticationManager│
│              │         │ (Spring Security)    │
│              │         └──────────┬───────────┘
│              │                    │
│              │                    ▼
│              │         ┌────────────────────┐
│              │         │ IUserService      │
│              │         │ (Verify User)     │
│              │         └────────┬──────────┘
│              │                  │
│              │                  ▼
│              │         ┌────────────────────┐
│              │         │ JwtUtil            │
│              │         │ (Generate Token)   │
│              │         └────────┬──────────┘
│              │                  │
│              │◄─────────────────┘
│              │ Token + Role
└──────────────┘
```

**Key Classes:**
- `JwtUtil`: Token generation and validation
- `JwtAuthenticationFilter`: Filter for incoming requests
- `SecurityConfig`: Spring Security configuration
- `AuthController`: Authentication endpoints

#### 2. **User Management Component**

```
UserController
    ↓
IUserService (Interface)
    ├─ getUserById()
    ├─ getUserByUsername()
    ├─ createUser()
    ├─ updateUser()
    ├─ deleteUser()
    └─ validateCredentials()
    ↓
UserServiceImpl (Implementation)
    ↓
UserRepository (JPA)
    ↓
User Entity (Database)
```

#### 3. **Parking Space Management Component**

```
ParkingSpaceController
    ↓
IParkingSpaceService
    ├─ getAllSpaces()
    ├─ getAvailableSpaces()
    ├─ getSpacesByType()
    ├─ createParkingSpace()
    ├─ updateSpace()
    ├─ updateAvailability()
    └─ deleteSpace()
    ↓
ParkingSpaceRepository
    ↓
ParkingSpace Entity
    ↓
parking_spaces Table
```

#### 4. **Vehicle Management Component**

```
VehicleController
    ↓
IVehicleService
    ├─ registerVehicle()
    ├─ getVehiclesByUser()
    ├─ updateVehicle()
    ├─ deactivateVehicle()
    └─ getVehicleDetails()
    ↓
VehicleRepository
    ↓
Vehicle Entity
    ↓
vehicles Table
```

#### 5. **Parking Entry/Exit Component**

```
ParkingEntryExitController
    ↓
IParkingEntryExitService
    ├─ recordEntry()
    │  ├─ Validate vehicle
    │  ├─ Check available space
    │  ├─ Create parking ticket
    │  └─ Update space status
    │
    └─ recordExit()
       ├─ Find active ticket
       ├─ Calculate duration
       ├─ Update ticket status
       ├─ Update space availability
       └─ Trigger billing
    ↓
Multiple Repositories
    ├─ ParkingTicketRepository
    ├─ ParkingSpaceRepository
    └─ VehicleRepository
```

#### 6. **Billing Component**

```
BillingController
    ↓
IBillingService
    ├─ generateBill()
    │  ├─ Calculate duration
    │  ├─ Apply rates
    │  ├─ Calculate tax
    │  ├─ Apply discounts
    │  └─ Create Bill entity
    │
    ├─ processPayment()
    ├─ getBillHistory()
    └─ generateInvoice()
    ↓
BillRepository
ParkingTicketRepository
    ↓
Bill Table
```

#### 7. **Reservation Component**

```
ReservationController
    ↓
IReservationService
    ├─ createReservation()
    ├─ cancelReservation()
    ├─ getReservations()
    ├─ checkAvailability()
    └─ confirmReservation()
    ↓
ReservationRepository
ParkingSpaceRepository
    ↓
Reservation Table
```

#### 8. **Smart Slot Allocation Component**

```
SlotAllocationController
    ↓
ISlotAllocationService
    ├─ allocateSlot()
    │  ├─ Apply allocation algorithm
    │  ├─ Consider vehicle type
    │  ├─ Check user preferences
    │  └─ Optimize for distance/parking rates
    │
    ├─ suggestAlternateSlots()
    └─ deallocateSlot()
    ↓
ParkingSpaceRepository
VehicleRepository
    ↓
Parking Space Table
```

---

## Data Flow Architecture

### Entry Flow (Vehicle Entry)

```
1. User initiates entry via App/UI
         │
         ▼
2. POST /api/parking-entry/record-entry
         │
         ▼
3. ParkingEntryExitController.recordEntry()
    ├─ Validates JWT Token
    ├─ Validates Request DTO
         │
         ▼
4. IParkingEntryExitService.recordEntry()
    ├─ Fetch Vehicle (VehicleRepository)
    ├─ Check Vehicle Active Status
    ├─ Get Available Parking Space
    ├─ Call ISlotAllocationService (if auto-allocation)
    ├─ Create ParkingTicket
    ├─ Update ParkingSpace availability
         │
         ▼
5. Database Transaction Commit
    ├─ INSERT INTO parking_tickets
    ├─ UPDATE parking_spaces SET availability_status='occupied'
         │
         ▼
6. Return Ticket Reference (TKT-XXXXXXXX)
    to Client with Parking Space Details
```

### Exit Flow (Vehicle Exit)

```
1. User initiates exit via App/UI
         │
         ▼
2. POST /api/parking-entry/record-exit
         │
         ▼
3. ParkingEntryExitController.recordExit()
    ├─ Validates JWT Token
    ├─ Validates Request DTO
         │
         ▼
4. IParkingEntryExitService.recordExit()
    ├─ Fetch Active Ticket
    ├─ Validate Ticket Status
    ├─ Calculate Duration (exit_time - entry_time)
    ├─ Update Ticket Status to CLOSED
    ├─ Free up ParkingSpace
         │
         ▼
5. IBillingService.generateBill()
    ├─ Get Rate from ParkingSpace
    ├─ Calculate Base Amount (duration × rate_per_hour)
    ├─ Calculate Tax (if applicable)
    ├─ Apply Discounts (if any)
    ├─ Calculate Total Amount
    ├─ Create Bill Entity
         │
         ▼
6. Database Transactions
    ├─ UPDATE parking_tickets SET status='CLOSED', exit_time=NOW()
    ├─ UPDATE parking_spaces SET availability_status='available'
    ├─ INSERT INTO bills
         │
         ▼
7. Return Bill Details & Invoice Number
```

### Billing Flow

```
ParkingTicket (EXIT recorded)
         │
         ▼
IBillingService.generateBill()
         │
    ┌────┴────┬──────────────┬──────────┐
    ▼         ▼              ▼          ▼
  Duration  RatePerHour  TaxRates  Discounts
    │         │              │          │
    └─────────┴──────────────┴──────────┘
             │
             ▼
      Calculation Engine
      ├─ DurationHours = Duration in Minutes / 60
      ├─ BaseAmount = DurationHours × RatePerHour
      ├─ TaxAmount = BaseAmount × TaxPercentage
      ├─ TotalBeforDiscount = BaseAmount + TaxAmount
      ├─ DiscountedAmount = TotalBeforeDiscount - DiscountAmount
      └─ FinalAmount = Max(0, DiscountedAmount)
             │
             ▼
         Bill Entity
      (Status: PENDING)
             │
             ▼
      Store in Database
             │
             ▼
      Return Bill with Status
```

---

## Database Design

### Entity Relationship Diagram (ERD)

```
┌────────────────────┐
│    parking_user    │
├────────────────────┤
│ user_id (PK)       │
│ user_name          │
│ user_email         │
│ password           │
│ role               │
│ license_plate      │
│ created_date       │
└──────────┬─────────┘
           │ (1:N)
      ┌────┴──────────────────────────────┐
      │                                   │
      ▼                                   ▼
┌────────────────────┐           ┌────────────────────┐
│    vehicles        │           │  parking_tickets   │
├────────────────────┤           ├────────────────────┤
│ vehicle_id (PK)    │           │ ticket_id (PK)     │
│ user_id (FK)       │───┐       │ user_id (FK)       │
│ license_plate      │   │       │ vehicle_id (FK)    │
│ vehicle_type       │   │       │ parking_space_id   │
│ vehicle_model      │   │       │ entry_time         │
│ vehicle_color      │   │       │ exit_time          │
│ registration_no    │   │       │ status             │
│ is_active          │   │       │ ticket_reference   │
│ created_at         │   │       │ created_at         │
│ updated_at         │   │       └────────┬───────────┘
└────────────────────┘   │                 │
                         │            ┌────┴────────────────────────┐
                         │            │                             │
                    ┌────▼────────────┴─────┐                        ▼
                    │                       │             ┌────────────────────┐
                    ▼                       ▼             │      bills         │
          ┌────────────────────┐  ┌─────────────────────────────────────────────────┤
          │ parking_spaces     │  │ bill_id (PK)       │
          ├────────────────────┤  │ invoice_number     │
          │ parking_space_id   │  │ user_id (FK)       │
          │ space_number       │  │ ticket_id (FK)     │
          │ location           │  │ rate_per_hour      │
          │ floor_number       │  │ duration_hours     │
          │ slot_type          │  │ base_amount        │
          │ availability_status│  │ tax_amount         │
          │ rate_per_hour      │  │ discount_amount    │
          │ is_active          │  │ total_amount       │
          │ created_at         │  │ status             │
          │ updated_at         │  │ payment_date       │
          └────────────────────┘  │ created_at         │
                                  └────────────────────┘

Optional Tables (Extended Features):
┌────────────────────┐    ┌────────────────────┐
│   reservations     │    │    feedback        │
├────────────────────┤    ├────────────────────┤
│ reservation_id     │    │ feedback_id        │
│ user_id (FK)       │    │ user_id (FK)       │
│ space_id (FK)      │    │ ticket_id (FK)     │
│ reservation_date   │    │ rating             │
│ desired_time       │    │ comment            │
│ status             │    │ created_at         │
│ created_at         │    │ updated_at         │
└────────────────────┘    └────────────────────┘
```

### Database Schema Features

**Indexing Strategy:**
```sql
-- Fast lookups
CREATE INDEX idx_user_email ON parking_user(user_email);
CREATE INDEX idx_vehicle_license_plate ON vehicles(license_plate);
CREATE INDEX idx_parking_space_availability ON parking_spaces(availability_status);

-- Query optimization
CREATE INDEX idx_ticket_status ON parking_tickets(status);
CREATE INDEX idx_bill_user_status ON bills(user_id, status);
CREATE INDEX idx_parking_timestamp ON parking_tickets(entry_time, exit_time);
```

**Views for Reporting:**
```
parking_dashboard        → Real-time parking stats
revenue_report          → Daily revenue analysis
user_parking_stats      → Per-user statistics
```

**Stored Procedures:**
```
get_occupancy_percentage()  → Calculate current occupancy
cleanup_expired_tickets()   → Auto-cleanup of stale tickets
```

---

## Security Architecture

### JWT Authentication Flow

```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │
       │ 1. POST /api/auth/login {username, password}
       ▼
┌──────────────────────────────────────────┐
│      AuthController.login()              │
│ ┌────────────────────────────────────┐   │
│ │  Validate Input (BasicValidation)  │   │
│ └────────────────────────────────────┘   │
└──────┬────────────────────────────────────┘
       │
       │ 2. AuthenticationManager.authenticate()
       ▼
┌──────────────────────────────────────────┐
│   Spring Security Authentication        │
│ ┌────────────────────────────────────┐   │
│ │ Load User Details (ICustomUserDet) │   │
│ │ Validate Credentials               │   │
│ └────────────────────────────────────┘   │
└──────┬────────────────────────────────────┘
       │
       │ 3. Authentication Success
       ▼
┌──────────────────────────────────────────┐
│      JwtUtil.generateToken()            │
│ ┌────────────────────────────────────┐   │
│ │ Header: {alg: HS256, typ: JWT}     │   │
│ │ Payload: {username, role, issuedAt,│   │
│ │          expiryAt, subject}        │   │
│ │ Signature: HMAC(header.payload)    │   │
│ └────────────────────────────────────┘   │
└──────┬────────────────────────────────────┘
       │
       │ 4. Return AuthResponse {token, role}
       ▼
┌──────────┐
│  Client  │ (Store JWT Token)
└─────┬────┘
      │
      │ 5. Subsequent Request: Authorization: Bearer <token>
      ▼
┌───────────────────────────────────────────┐
│  JwtAuthenticationFilter                 │
│ ┌─────────────────────────────────────┐  │
│ │ 1. Extract Token from Header        │  │
│ │ 2. Validate Token (not expired)     │  │
│ │ 3. Extract Claims (username, role)  │  │
│ │ 4. Create Authentication Object     │  │
│ │ 5. Set in SecurityContext           │  │
│ └─────────────────────────────────────┘  │
└──────┬───────────────────────────────────┘
       │
       │ 6. Request Processed with User Context
       ▼
    ✓ Access Granted or ✗ Access Denied
```

### Security Configuration

```yaml
Security Rules:
├─ Public Endpoints (No Auth Required)
│  ├─ POST /api/users/register
│  └─ POST /api/auth/login
│
├─ Admin-Only Endpoints
│  └─ /api/parkingspace/** (All operations)
│
├─ Authenticated Endpoints (All others)
│  └─ Requires valid JWT token in header
│
└─ CORS Configuration
   └─ Restricted origins for API access
```

### Password Security

```
User Registration Flow:
┌──────────────────────────────────────┐
│  User submits password (plaintext)   │
└───────────┬─────────────────────────┘
            │
            ▼
┌──────────────────────────────────────┐
│  PasswordEncoder.encode()            │
│  (BCryptPasswordEncoder)             │
│  ├─ Generate random salt             │
│  ├─ Apply iterations                 │
│  └─ Create hash                      │
└───────────┬─────────────────────────┘
            │
            ▼
┌──────────────────────────────────────┐
│  Store hashed password in DB         │
│  Plaintext password discarded        │
└──────────────────────────────────────┘

Login Flow:
┌──────────────────────────────────────┐
│  User submits password               │
└───────────┬─────────────────────────┘
            │
            ▼
┌──────────────────────────────────────┐
│  Fetch stored hash from DB           │
└───────────┬─────────────────────────┘
            │
            ▼
┌──────────────────────────────────────┐
│  PasswordEncoder.matches()           │
│  (Verify submitted against hash)     │
└───────────┬─────────────────────────┘
            │
      ┌─────┴──────┐
      ▼            ▼
    Match      No Match
      │            │
      ✓            ✗
   Grant        Reject
```

---

## Integration Points

### External Integrations (Future)

```
Parking System ────── Payment Gateway
                      (e.g., Stripe, PayPal)
                
                  ───── SMS/Email Service
                      (Notifications)
                
                  ───── File Storage
                      (Photos - S3/Blob)
                
                  ───── Analytics
                      (Third-party tools)
                
                  ───── ANPR System
                      (License Plate Recognition)
```

### API Gateway Pattern (Future Deployment)

```
                   ┌─────────────────┐
                   │   API Gateway   │
                   │  (Kong/Zuul)    │
                   └────────┬────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
   Auth Service      Parking Service     Billing Service
   (Microservice)    (This project)     (Microservice)
```

---

## Deployment Architecture

### Container Deployment (Docker)

```dockerfile
# Parking System Docker Image
Dockerfile:
├─ Base Image: openjdk:17-jdk-slim
├─ Copy Application JAR
├─ Expose Port: 8080
├─ Environment Variables
│  ├─ SPRING_DATASOURCE_URL
│  ├─ SPRING_DATASOURCE_USERNAME
│  ├─ SPRING_DATASOURCE_PASSWORD
│  └─ JWT_SECRET_KEY
└─ CMD: java -jar parking-system.jar
```

### Kubernetes Deployment (K8s)

```yaml
# Deployment manifest
apiVersion: apps/v1
kind: Deployment
metadata:
  name: parking-system
spec:
  replicas: 3  # For high availability
  selector:
    matchLabels:
      app: parking-system
  template:
    metadata:
      labels:
        app: parking-system
    spec:
      containers:
      - name: parking-system
        image: parking-system:1.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: db-secrets
              key: url
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
# Service for load balancing
apiVersion: v1
kind: Service
metadata:
  name: parking-system-service
spec:
  type: LoadBalancer
  ports:
  - protocol: TCP
    port: 80
    targetPort: 8080
  selector:
    app: parking-system
```

---

## Scalability & Performance

### Horizontal Scalability

```
Load Balancer
      │
      ├─ Instance 1 (Parking System)
      ├─ Instance 2 (Parking System)
      ├─ Instance 3 (Parking System)
      └─ Instance N (Parking System)
      
      All connected to:
      └─ PostgreSQL Database (Master)
         └─ Read Replicas (Optional)
```

### Performance Optimization

1. **Database Optimization**
   - Proper indexing on frequently queried columns
   - Query optimization using EXPLAIN ANALYZE
   - Connection pooling (HikariCP)
   - Connection pool size: 10-20 connections

2. **Caching Strategy**
   ```
   Application Cache:
   ├─ Parking space availability (Redis)
   ├─ User details (Spring Cache)
   └─ Static configurations
   ```

3. **API Response Times**
   - Target: < 200ms for most endpoints
   - Critical endpoints: < 100ms
   - Long-running: Async processing

4. **Query Optimization**
   ```sql
   -- Use indexes effectively
   SELECT * FROM parking_spaces 
   WHERE availability_status = 'available'
   AND slot_type = 'NORMAL'
   -- Uses: idx_parking_spaces_availability, idx_parking_spaces_slot_type
   
   -- Avoid N+1 queries
   -- Use JOIN instead of multiple queries
   ```

---

## Monitoring & Observability

### Health Checks

```
Spring Boot Actuator Endpoints:
├─ GET /actuator/health           → Application health
├─ GET /actuator/metrics          → System metrics
├─ GET /actuator/loggers          → Logger configuration
└─ GET /actuator/env              → Environment properties
```

### Logging Strategy

```
Log Levels (Hierarchical):
├─ ERROR: Critical failures (DB down, Auth failure)
├─ WARN: Deprecations, potential issues
├─ INFO: Request logs, important events
└─ DEBUG: Detailed execution flow

Structured Logging:
{
  "timestamp": "2026-06-07T10:30:45.123Z",
  "level": "ERROR",
  "logger": "com.parkingSystem.service.ParkingEntryExitService",
  "message": "Failed to allocate parking space",
  "userId": 123,
  "vehicleId": 456,
  "exception": "NoAvailableSpaceException",
  "stackTrace": "..."
}
```

### Metrics to Monitor

```
Application Metrics:
├─ Request Count (by endpoint)
├─ Response Time (p50, p95, p99)
├─ Error Rate
├─ Active Users
├─ Transactions per second

Business Metrics:
├─ Parking occupancy rate
├─ Revenue per day
├─ Average parking duration
├─ Payment success rate
└─ User registration rate

Infrastructure Metrics:
├─ CPU Usage
├─ Memory Usage
├─ Disk I/O
├─ Database connection pool usage
└─ Network I/O
```

### Alerting Rules

```
Alert Conditions:
├─ Error Rate > 5%
├─ Response Time p95 > 500ms
├─ Database Connection Pool > 80%
├─ Occupancy > 95%
├─ Failed Transactions > 10 per minute
└─ Deployment failures
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-07 | Initial HLD document |

---

## Document Revision

**Author:** Architecture Team  
**Last Reviewed:** 2026-06-07  
**Next Review:** 2026-09-07  

---

## Appendix A: Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Framework | Spring Boot | 3.3.1 |
| ORM | Hibernate/JPA | 6.x |
| Database | PostgreSQL | 12+ |
| Authentication | JWT + Spring Security | 5.8+ |
| Build Tool | Maven | 3.8+ |
| Java Version | OpenJDK | 17 LTS |
| Testing | JUnit 5, Mockito | 5.x, 4.x+ |
| Logging | SLF4J + Logback | 2.x, 1.4+ |
| Documentation | Springdoc OpenAPI | 2.x |

---

**End of Document**

