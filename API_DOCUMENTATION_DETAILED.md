# Parking System - API Documentation

**Version:** 1.0  
**Last Updated:** June 7, 2026  
**Base URL:** `http://localhost:8080` (Development)  
**API Version:** v1  

---

## Table of Contents

1. [Authentication](#authentication)
2. [User Management](#user-management)
3. [Vehicle Management](#vehicle-management)
4. [Parking Space Management](#parking-space-management)
5. [Parking Entry/Exit](#parking-entryexit)
6. [Reservations](#reservations)
7. [Billing & Invoicing](#billing--invoicing)
8. [Slot Allocation](#slot-allocation)
9. [Feedback Management](#feedback-management)
10. [Error Handling](#error-handling)
11. [Response Formats](#response-formats)

---

## Authentication

All protected endpoints require a JWT token in the Authorization header.

### Request Header Format
```
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

### Login

**Endpoint:** `POST /api/auth/login`

**Access Level:** Public (No authentication required)

**Request Body:**
```json
{
  "username": "john_doe",
  "password": "securePassword123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "role": "USER"
}
```

**Response (401 Unauthorized):**
```json
{
  "status": 401,
  "message": "Invalid credentials",
  "timestamp": "2026-06-07T10:30:45.123Z"
}
```

**JWT Token Structure:**
```
Header: {
  "alg": "HS256",
  "typ": "JWT"
}

Payload: {
  "username": "john_doe",
  "role": "USER",
  "iat": 1717762245,
  "exp": 1717848645
}
```

---

## User Management

### Register User

**Endpoint:** `POST /api/users/register`

**Access Level:** Public (No authentication required)

**Request Body:**
```json
{
  "userName": "john_doe",
  "userEmail": "john@example.com",
  "password": "securePassword123",
  "licencePlate": "KA-01-AB-1234"
}
```

**Response (201 Created):**
```json
{
  "userId": 1,
  "userName": "john_doe",
  "userEmail": "john@example.com",
  "licencePlate": "KA-01-AB-1234",
  "role": "USER",
  "registrationDate": "2026-06-07",
  "createdDate": "2026-06-07T10:30:45.123Z"
}
```

**Response (400 Bad Request):**
```json
{
  "status": 400,
  "message": "Email already exists",
  "timestamp": "2026-06-07T10:30:45.123Z"
}
```

**Validation Rules:**
- `userName`: Required, unique, min 3 characters
- `userEmail`: Required, unique, valid email format
- `password`: Required, min 8 characters
- `licencePlate`: Required, valid format

---

### Get User Profile

**Endpoint:** `GET /api/users/{userId}`

**Access Level:** Authenticated (User or Admin)

**Path Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| userId | Long | Unique user identifier |

**Response (200 OK):**
```json
{
  "userId": 1,
  "userName": "john_doe",
  "userEmail": "john@example.com",
  "licencePlate": "KA-01-AB-1234",
  "role": "USER",
  "registrationDate": "2026-06-07",
  "createdDate": "2026-06-07T10:30:45.123Z"
}
```

**Response (403 Forbidden):**
```json
{
  "status": 403,
  "message": "Access denied",
  "timestamp": "2026-06-07T10:30:45.123Z"
}
```

---

### Update User Profile

**Endpoint:** `PUT /api/users/{userId}`

**Access Level:** Authenticated (User or Admin)

**Request Body:**
```json
{
  "userName": "john_doe_updated",
  "userEmail": "newemail@example.com",
  "licencePlate": "KA-01-AB-5678"
}
```

**Response (200 OK):**
```json
{
  "userId": 1,
  "userName": "john_doe_updated",
  "userEmail": "newemail@example.com",
  "licencePlate": "KA-01-AB-5678",
  "role": "USER"
}
```

---

### Delete User

**Endpoint:** `DELETE /api/users/{userId}`

**Access Level:** Authenticated (Admin only)

**Response (204 No Content):**
```
No body returned
```

---

## Vehicle Management

### Register Vehicle

**Endpoint:** `POST /api/vehicles/register`

**Access Level:** Authenticated (User)

**Request Body:**
```json
{
  "userId": 1,
  "licensePlate": "KA-01-AB-1234",
  "vehicleType": "CAR",
  "vehicleModel": "Honda City",
  "vehicleColor": "Silver",
  "registrationNumber": "KA-1234-5678"
}
```

**Valid Vehicle Types:**
- `CAR`: Sedan/SUV vehicles
- `BIKE`: Two-wheeler vehicles
- `EV`: Electric vehicles

**Response (201 Created):**
```json
{
  "vehicleId": 1,
  "userId": 1,
  "licensePlate": "KA-01-AB-1234",
  "vehicleType": "CAR",
  "vehicleModel": "Honda City",
  "vehicleColor": "Silver",
  "registrationNumber": "KA-1234-5678",
  "isActive": true,
  "createdAt": "2026-06-07T10:30:45.123Z",
  "updatedAt": "2026-06-07T10:30:45.123Z"
}
```

---

### Get User Vehicles

**Endpoint:** `GET /api/vehicles/user/{userId}`

**Access Level:** Authenticated (User)

**Query Parameters:**
| Parameter | Type | Default | Description |
|-----------|------|---------|-------------|
| page | Integer | 0 | Page number (0-indexed) |
| size | Integer | 10 | Items per page |
| sortBy | String | createdAt | Sort field |
| sortDirection | String | DESC | ASC or DESC |

**Example:** `GET /api/vehicles/user/1?page=0&size=10&sortBy=createdAt&sortDirection=DESC`

**Response (200 OK):**
```json
{
  "content": [
    {
      "vehicleId": 1,
      "licensePlate": "KA-01-AB-1234",
      "vehicleType": "CAR",
      "vehicleModel": "Honda City",
      "vehicleColor": "Silver",
      "isActive": true
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

---

### Update Vehicle

**Endpoint:** `PUT /api/vehicles/{vehicleId}`

**Access Level:** Authenticated (Owner)

**Request Body:**
```json
{
  "vehicleModel": "Honda City 2025",
  "vehicleColor": "Black"
}
```

**Response (200 OK):**
```json
{
  "vehicleId": 1,
  "licensePlate": "KA-01-AB-1234",
  "vehicleModel": "Honda City 2025",
  "vehicleColor": "Black",
  "vehicleType": "CAR",
  "updatedAt": "2026-06-07T11:00:00.123Z"
}
```

---

### Deactivate Vehicle

**Endpoint:** `DELETE /api/vehicles/{vehicleId}`

**Access Level:** Authenticated (Owner)

**Response (200 OK):**
```json
{
  "message": "Vehicle deactivated successfully",
  "vehicleId": 1
}
```

---

## Parking Space Management

### Get All Parking Spaces

**Endpoint:** `GET /api/parkingspace/all`

**Access Level:** Authenticated

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | Integer | Page number (0-indexed) |
| size | Integer | Items per page |
| location | String | Filter by location |
| slotType | String | Filter by type (NORMAL, VIP, HANDICAPPED) |
| availability | String | Filter by status (available, occupied) |

**Example:** `GET /api/parkingspace/all?page=0&size=20&availability=available&slotType=NORMAL`

**Response (200 OK):**
```json
{
  "content": [
    {
      "parkingSpaceId": 1,
      "spaceNumber": "A1-001",
      "location": "A1",
      "floorNumber": 1,
      "slotType": "NORMAL",
      "availabilityStatus": "available",
      "ratePerHour": 50.00,
      "isActive": true
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 50,
    "totalPages": 3
  }
}
```

---

### Get Available Spaces

**Endpoint:** `GET /api/parkingspace/available`

**Access Level:** Authenticated

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| slotType | String | Optional: Filter by type |
| location | String | Optional: Filter by location |

**Example:** `GET /api/parkingspace/available?slotType=NORMAL&location=A1`

**Response (200 OK):**
```json
{
  "availableSpaces": [
    {
      "parkingSpaceId": 1,
      "spaceNumber": "A1-001",
      "location": "A1",
      "floorNumber": 1,
      "slotType": "NORMAL",
      "ratePerHour": 50.00
    }
  ],
  "totalAvailable": 15,
  "totalCapacity": 50
}
```

---

### Create Parking Space (Admin Only)

**Endpoint:** `POST /api/parkingspace/create`

**Access Level:** Admin

**Request Body:**
```json
{
  "spaceNumber": "A1-051",
  "location": "A1",
  "floorNumber": 5,
  "slotType": "VIP",
  "availabilityStatus": "available",
  "ratePerHour": 100.00,
  "notes": "Premium parking with climate control"
}
```

**Response (201 Created):**
```json
{
  "parkingSpaceId": 51,
  "spaceNumber": "A1-051",
  "location": "A1",
  "floorNumber": 5,
  "slotType": "VIP",
  "ratePerHour": 100.00,
  "isActive": true,
  "createdAt": "2026-06-07T10:30:45.123Z"
}
```

---

### Update Parking Space (Admin Only)

**Endpoint:** `PUT /api/parkingspace/{spaceId}`

**Access Level:** Admin

**Request Body:**
```json
{
  "ratePerHour": 75.00,
  "notes": "Updated notes"
}
```

**Response (200 OK):**
```json
{
  "parkingSpaceId": 1,
  "spaceNumber": "A1-001",
  "ratePerHour": 75.00,
  "updatedAt": "2026-06-07T11:00:00.123Z"
}
```

---

### Get Parking Summary Statistics

**Endpoint:** `GET /api/parkingspace/statistics`

**Access Level:** Admin

**Response (200 OK):**
```json
{
  "totalSpaces": 50,
  "availableSpaces": 15,
  "occupiedSpaces": 35,
  "occupancyPercentage": 70.0,
  "bySlotType": {
    "NORMAL": {
      "total": 30,
      "available": 10,
      "occupied": 20
    },
    "VIP": {
      "total": 15,
      "available": 4,
      "occupied": 11
    },
    "HANDICAPPED": {
      "total": 5,
      "available": 1,
      "occupied": 4
    }
  },
  "byLocation": {
    "A1": { "available": 5, "occupied": 25 },
    "A2": { "available": 5, "occupied": 5 },
    "B1": { "available": 5, "occupied": 5 }
  }
}
```

---

## Parking Entry/Exit

### Record Vehicle Entry

**Endpoint:** `POST /api/parking-entry/record-entry`

**Access Level:** Authenticated (User)

**Request Body:**
```json
{
  "userId": 1,
  "vehicleId": 1,
  "parkingSpaceId": 1,
  "entryPhotoUrl": "https://cdn.example.com/entry-photo-123.jpg"
}
```

**Response (201 Created):**
```json
{
  "ticketId": 100,
  "ticketReference": "TKT-ABC123DEF456",
  "userId": 1,
  "vehicleId": 1,
  "vehicleLicensePlate": "KA-01-AB-1234",
  "parkingSpaceId": 1,
  "spaceNumber": "A1-001",
  "location": "A1",
  "entryTime": "2026-06-07T10:30:45.123Z",
  "status": "ACTIVE",
  "ratePerHour": 50.00,
  "message": "Welcome! Your parking ticket has been generated."
}
```

**Response (400 Bad Request):**
```json
{
  "status": 400,
  "message": "Space not available or vehicle already parked",
  "timestamp": "2026-06-07T10:30:45.123Z"
}
```

---

### Record Vehicle Exit

**Endpoint:** `POST /api/parking-entry/record-exit`

**Access Level:** Authenticated (User)

**Request Body:**
```json
{
  "ticketId": 100,
  "exitPhotoUrl": "https://cdn.example.com/exit-photo-123.jpg"
}
```

**Response (200 OK):**
```json
{
  "ticketId": 100,
  "ticketReference": "TKT-ABC123DEF456",
  "exitTime": "2026-06-07T12:30:00.123Z",
  "durationMinutes": 120,
  "durationHours": 2.0,
  "status": "CLOSED",
  "billGenerated": {
    "billId": 200,
    "invoiceNumber": "INV-2026-06-07-001",
    "baseAmount": 100.00,
    "taxAmount": 18.00,
    "totalAmount": 118.00,
    "status": "PENDING"
  }
}
```

---

### Get Active Parking Tickets

**Endpoint:** `GET /api/parking-entry/active-tickets/{userId}`

**Access Level:** Authenticated (User or Admin)

**Response (200 OK):**
```json
{
  "activeTickets": [
    {
      "ticketId": 100,
      "ticketReference": "TKT-ABC123DEF456",
      "vehicleLicensePlate": "KA-01-AB-1234",
      "spaceNumber": "A1-001",
      "entryTime": "2026-06-07T10:30:45.123Z",
      "parkedDurationMinutes": 45
    }
  ],
  "totalActive": 1
}
```

---

### Get Parking History

**Endpoint:** `GET /api/parking-entry/history/{userId}`

**Access Level:** Authenticated (User)

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| page | Integer | Page number (0-indexed) |
| size | Integer | Items per page |
| fromDate | String | ISO 8601 date (YYYY-MM-DD) |
| toDate | String | ISO 8601 date (YYYY-MM-DD) |

**Example:** `GET /api/parking-entry/history/1?page=0&size=10&fromDate=2026-06-01&toDate=2026-06-07`

**Response (200 OK):**
```json
{
  "content": [
    {
      "ticketId": 95,
      "ticketReference": "TKT-XYZ789UVW012",
      "vehicleLicensePlate": "KA-01-AB-1234",
      "spaceNumber": "A1-005",
      "entryTime": "2026-06-06T14:00:00.123Z",
      "exitTime": "2026-06-06T16:30:00.123Z",
      "durationMinutes": 150,
      "status": "CLOSED"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

---

## Reservations

### Create Reservation

**Endpoint:** `POST /api/reservations/create`

**Access Level:** Authenticated (User)

**Request Body:**
```json
{
  "userId": 1,
  "parkingSpaceId": 5,
  "reservationDate": "2026-06-10",
  "desiredTime": "09:00:00"
}
```

**Response (201 Created):**
```json
{
  "reservationId": 1,
  "userId": 1,
  "parkingSpaceId": 5,
  "spaceNumber": "A1-005",
  "reservationDate": "2026-06-10",
  "desiredTime": "09:00:00",
  "status": "CONFIRMED",
  "createdAt": "2026-06-07T10:30:45.123Z"
}
```

---

### Get User Reservations

**Endpoint:** `GET /api/reservations/user/{userId}`

**Access Level:** Authenticated (User)

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| status | String | Filter (CONFIRMED, CANCELLED, COMPLETED) |
| page | Integer | Page number |
| size | Integer | Items per page |

**Response (200 OK):**
```json
{
  "content": [
    {
      "reservationId": 1,
      "spaceNumber": "A1-005",
      "reservationDate": "2026-06-10",
      "desiredTime": "09:00:00",
      "status": "CONFIRMED"
    }
  ],
  "totalReservations": 1
}
```

---

### Cancel Reservation

**Endpoint:** `DELETE /api/reservations/{reservationId}`

**Access Level:** Authenticated (User)

**Response (200 OK):**
```json
{
  "message": "Reservation cancelled successfully",
  "reservationId": 1,
  "refundAmount": 50.00
}
```

---

## Billing & Invoicing

### Get Bill Details

**Endpoint:** `GET /api/billing/{billId}`

**Access Level:** Authenticated (User or Admin)

**Response (200 OK):**
```json
{
  "billId": 200,
  "invoiceNumber": "INV-2026-06-07-001",
  "userId": 1,
  "ticketId": 100,
  "parkingDuration": {
    "hours": 2,
    "minutes": 30
  },
  "ratePerHour": 50.00,
  "calculations": {
    "durationHours": 2.5,
    "baseAmount": 125.00,
    "taxPercentage": 18,
    "taxAmount": 22.50,
    "discountAmount": 0.00,
    "totalBeforeDiscount": 147.50,
    "finalAmount": 147.50
  },
  "status": "PENDING",
  "paymentMethod": null,
  "paymentDate": null,
  "billingDate": "2026-06-07T12:30:00.123Z",
  "notes": "Standard parking",
  "createdAt": "2026-06-07T12:30:45.123Z"
}
```

---

### Get User Bills

**Endpoint:** `GET /api/billing/user/{userId}`

**Access Level:** Authenticated (User)

**Query Parameters:**
| Parameter | Type | Description |
|-----------|------|-------------|
| status | String | Filter (PENDING, PAID, CANCELLED) |
| page | Integer | Page number |
| size | Integer | Items per page |

**Example:** `GET /api/billing/user/1?status=PENDING&page=0&size=10`

**Response (200 OK):**
```json
{
  "content": [
    {
      "billId": 200,
      "invoiceNumber": "INV-2026-06-07-001",
      "totalAmount": 147.50,
      "status": "PENDING",
      "billingDate": "2026-06-07T12:30:00.123Z"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 5,
    "totalPages": 1
  }
}
```

---

### Process Payment

**Endpoint:** `POST /api/billing/{billId}/pay`

**Access Level:** Authenticated (User)

**Request Body:**
```json
{
  "paymentMethod": "CREDIT_CARD",
  "transactionId": "TXN-123456789"
}
```

**Response (200 OK):**
```json
{
  "billId": 200,
  "invoiceNumber": "INV-2026-06-07-001",
  "status": "PAID",
  "totalAmount": 147.50,
  "paymentMethod": "CREDIT_CARD",
  "paymentDate": "2026-06-07T13:00:00.123Z",
  "message": "Payment processed successfully"
}
```

---

### Generate Invoice PDF (Future)

**Endpoint:** `GET /api/billing/{billId}/invoice`

**Access Level:** Authenticated (User)

**Response:**
```
Returns PDF binary data
Content-Type: application/pdf
Content-Disposition: attachment; filename="INV-2026-06-07-001.pdf"
```

---

## Slot Allocation

### Get Smart Slot Suggestions

**Endpoint:** `POST /api/slot-allocation/suggest`

**Access Level:** Authenticated (User)

**Request Body:**
```json
{
  "vehicleType": "CAR",
  "preferredSlotType": "NORMAL",
  "userId": 1,
  "location": "A1"
}
```

**Response (200 OK):**
```json
{
  "primarySuggestion": {
    "parkingSpaceId": 1,
    "spaceNumber": "A1-001",
    "location": "A1",
    "floorNumber": 1,
    "slotType": "NORMAL",
    "ratePerHour": 50.00,
    "matchScore": 0.95
  },
  "alternateSuggestions": [
    {
      "parkingSpaceId": 2,
      "spaceNumber": "A1-002",
      "location": "A1",
      "floorNumber": 1,
      "slotType": "NORMAL",
      "ratePerHour": 50.00,
      "matchScore": 0.90
    }
  ],
  "allocationAlgorithm": "DISTANCE_AND_RATE_OPTIMIZED"
}
```

---

### Allocate Slot Automatically

**Endpoint:** `POST /api/slot-allocation/allocate-automatic`

**Access Level:** Authenticated (User)

**Request Body:**
```json
{
  "vehicleId": 1,
  "vehicleType": "CAR",
  "userId": 1
}
```

**Response (200 OK):**
```json
{
  "allocationStatus": "SUCCESS",
  "allocatedSpaceId": 1,
  "spaceNumber": "A1-001",
  "location": "A1",
  "ratePerHour": 50.00,
  "reservationValid": "2026-06-07T10:45:45.123Z"
}
```

---

## Feedback Management

### Submit Feedback

**Endpoint:** `POST /api/feedback/submit`

**Access Level:** Authenticated (User)

**Request Body:**
```json
{
  "userId": 1,
  "ticketId": 100,
  "rating": 4,
  "comment": "Good parking experience but a bit crowded. Will visit again!"
}
```

**Valid Ratings:** 1-5 stars

**Response (201 Created):**
```json
{
  "feedbackId": 1,
  "userId": 1,
  "ticketId": 100,
  "rating": 4,
  "comment": "Good parking experience but a bit crowded. Will visit again!",
  "createdAt": "2026-06-07T10:30:45.123Z"
}
```

---

### Get Feedback Statistics

**Endpoint:** `GET /api/feedback/statistics`

**Access Level:** Admin

**Response (200 OK):**
```json
{
  "totalFeedback": 150,
  "averageRating": 4.2,
  "ratingDistribution": {
    "5": 60,
    "4": 50,
    "3": 25,
    "2": 10,
    "1": 5
  },
  "positivePercentage": 73.3,
  "negativePercentage": 10.0,
  "neutralPercentage": 16.7
}
```

---

## Error Handling

### Standard Error Response Format

```json
{
  "status": 400,
  "message": "Invalid request parameter",
  "errorCode": "INVALID_INPUT",
  "timestamp": "2026-06-07T10:30:45.123Z",
  "path": "/api/parking-entry/record-entry",
  "details": {
    "field": "vehicleId",
    "error": "Vehicle not found"
  }
}
```

### HTTP Status Codes

| Code | Meaning | Example |
|------|---------|---------|
| 200 | OK | Successful GET request |
| 201 | Created | Resource successfully created |
| 204 | No Content | Successful DELETE operation |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Invalid/missing JWT token |
| 403 | Forbidden | Insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists (duplicate) |
| 500 | Internal Server Error | Server error |

### Common Error Codes

| Code | Message | Cause |
|------|---------|-------|
| INVALID_INPUT | Invalid request data | Validation failed |
| RESOURCE_NOT_FOUND | Resource not found | ID doesn't exist |
| DUPLICATE_RESOURCE | Resource already exists | Unique constraint violated |
| UNAUTHORIZED | Invalid credentials | Wrong username/password |
| ACCESS_DENIED | Insufficient permissions | User role not authorized |
| NO_AVAILABLE_SPACE | No parking space available | All spaces occupied |
| INVALID_TOKEN | JWT token expired/invalid | Token validation failed |
| INTERNAL_ERROR | Internal server error | Unexpected error |

---

## Response Formats

### Paginated Response

```json
{
  "content": [
    { "id": 1, "name": "Item 1" },
    { "id": 2, "name": "Item 2" }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 100,
    "totalPages": 10,
    "isFirst": true,
    "isLast": false
  }
}
```

### Batch Response

```json
{
  "successCount": 50,
  "failureCount": 5,
  "failures": [
    {
      "id": 3,
      "error": "Invalid format"
    }
  ]
}
```

### List Response

```json
{
  "items": [
    { "id": 1, "name": "Item 1" },
    { "id": 2, "name": "Item 2" }
  ],
  "count": 2
}
```

---

## Rate Limiting (Future)

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1717765200
X-RateLimit-Retry-After: 3600
```

---

## API Versioning Strategy

**URL Structure:** `/api/v1/...`

**Future Versions:**
- v2: Breaking changes with new major features
- v1.1: Backward-compatible new features (minor version)

**Deprecation Notice:**
```
Deprecated-API: true
Sunset: 2026-12-31T23:59:59Z
```

---

## Testing API Endpoints

### Using cURL

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "securePassword123"
  }'

# Get All Parking Spaces (with token)
curl -X GET http://localhost:8080/api/parkingspace/all \
  -H "Authorization: Bearer <TOKEN>"

# Record Vehicle Entry
curl -X POST http://localhost:8080/api/parking-entry/record-entry \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "vehicleId": 1,
    "parkingSpaceId": 1
  }'
```

### Using Postman

1. **Set up environment:**
   - Create variable: `token` = JWT token from login response
   - Create variable: `base_url` = http://localhost:8080

2. **Use in requests:**
   - Authorization Tab: Bearer Token → `{{token}}`
   - POST body uses `{{base_url}}/api/...`

---

## API Performance Specifications

| Endpoint | Target Response Time | Max Records |
|----------|---------------------|------------|
| GET /parkingspace/all | < 200ms | 10,000 |
| POST /parking-entry/record-entry | < 100ms | N/A |
| GET /parking-entry/history | < 300ms | 1,000 |
| POST /billing/*/pay | < 150ms | N/A |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-07 | Initial API documentation |

---

**Document Prepared By:** Development Team  
**Last Updated:** 2026-06-07  
**Next Review:** 2026-09-07

**End of Document**

