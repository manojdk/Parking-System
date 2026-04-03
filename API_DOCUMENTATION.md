# 🚗 Parking System - API Documentation

## 📌 Base URL
```
http://localhost:8080/api
```

---

## 🚙 VEHICLE MANAGEMENT APIs

### 1. Register Vehicle
```http
POST /vehicles/register
Content-Type: application/json

{
  "userId": 1,
  "licensePlate": "KA-01-AB-1234",
  "vehicleType": "CAR",
  "vehicleModel": "Honda Civic",
  "vehicleColor": "Silver",
  "registrationNumber": "REG-001"
}

Response 201:
{
  "status": "success",
  "message": "Vehicle registered successfully",
  "vehicle": {
    "vehicleId": 1,
    "userId": 1,
    "licensePlate": "KA-01-AB-1234",
    "vehicleType": "CAR",
    "vehicleModel": "Honda Civic",
    "vehicleColor": "Silver",
    "registrationNumber": "REG-001",
    "isActive": true
  }
}
```

### 2. Get Vehicle by ID
```http
GET /vehicles/{vehicleId}

Response 200:
{
  "vehicleId": 1,
  "userId": 1,
  "licensePlate": "KA-01-AB-1234",
  ...
}
```

### 3. Get Vehicle by License Plate
```http
GET /vehicles/license-plate/{licensePlate}

Response 200: Vehicle object
```

### 4. Get All User Vehicles
```http
GET /vehicles/user/{userId}

Response 200:
{
  "status": "success",
  "count": 2,
  "vehicles": [...]
}
```

### 5. Update Vehicle
```http
PUT /vehicles/{vehicleId}?vehicleModel=NewModel&vehicleColor=Red

Response 200: Updated vehicle object
```

### 6. Deactivate Vehicle
```http
DELETE /vehicles/{vehicleId}

Response 200:
{
  "status": "success",
  "message": "Vehicle deactivated successfully"
}
```

---

## 🅿️ PARKING SLOT MANAGEMENT APIs

### 1. Find Nearest Available Slot
```http
GET /slots/find-nearest?location=A1&slotType=NORMAL

Response 200:
{
  "status": "success",
  "message": "Nearest slot found",
  "slot": {
    "parkingSpaceId": 1,
    "spaceNumber": "A1-001",
    "location": "A1",
    "slotType": "NORMAL",
    "availabilityStatus": "available",
    "ratePerHour": 50.00,
    "floorNumber": 1
  }
}
```

### 2. Get Available Slots by Type
```http
GET /slots/available-by-type/{slotType}

Parameters:
- slotType: NORMAL, VIP, HANDICAPPED

Response 200:
{
  "status": "success",
  "slotType": "NORMAL",
  "count": 15,
  "slots": [...]
}
```

### 3. Get Available Slots by Location
```http
GET /slots/available-by-location?location=A1

Response 200:
{
  "status": "success",
  "location": "A1",
  "count": 5,
  "slots": [...]
}
```

### 4. Get All Available Slots
```http
GET /slots/available-all

Response 200:
{
  "status": "success",
  "totalAvailable": 45,
  "slots": [...]
}
```

### 5. Get Real-Time Availability Summary (Dashboard)
```http
GET /slots/availability-summary

Response 200:
{
  "status": "success",
  "summary": {
    "totalAvailableSlots": 45,
    "normalSlotsAvailable": 30,
    "vipSlotsAvailable": 10,
    "handicappedSlotsAvailable": 5
  }
}
```

### 6. Recommend Slot by Vehicle Type
```http
GET /slots/recommend?location=A1&vehicleType=CAR

Response 200: Recommended slot object
```

### 7. Check Slot Availability
```http
GET /slots/check-availability/{parkingSpaceId}

Response 200:
{
  "status": "success",
  "parkingSpaceId": 1,
  "isAvailable": true
}
```

---

## 🎫 PARKING ENTRY/EXIT APIs

### 1. Record Vehicle Entry
```http
POST /parking/entry
Content-Type: application/json

{
  "userId": 1,
  "vehicleId": 1,
  "parkingSpaceId": 1
}

Response 201:
{
  "status": "success",
  "message": "Vehicle entry recorded successfully",
  "ticket": {
    "ticketId": 1,
    "ticketReference": "TKT-A1B2C3D4E5F6",
    "userId": 1,
    "vehicleId": 1,
    "parkingSpaceId": 1,
    "entryTime": "2026-03-19T14:30:00",
    "exitTime": null,
    "durationMinutes": null,
    "status": "ACTIVE"
  }
}
```

### 2. Record Vehicle Exit
```http
POST /parking/exit?ticketReference=TKT-A1B2C3D4E5F6

Response 200:
{
  "status": "success",
  "message": "Vehicle exit recorded successfully",
  "ticket": {
    "ticketId": 1,
    "ticketReference": "TKT-A1B2C3D4E5F6",
    "entryTime": "2026-03-19T14:30:00",
    "exitTime": "2026-03-19T15:45:00",
    "durationMinutes": 75,
    "status": "CLOSED"
  }
}
```

### 3. Get Ticket Details
```http
GET /parking/ticket/{ticketReference}

Response 200: Ticket object
```

### 4. Get All Active Parking Sessions
```http
GET /parking/active-sessions

Response 200:
{
  "status": "success",
  "count": 12,
  "activeParking": [...]
}
```

### 5. Get User Parking History
```http
GET /parking/history/{userId}

Response 200:
{
  "status": "success",
  "count": 25,
  "parkingHistory": [...]
}
```

### 6. Get User's Current Active Sessions
```http
GET /parking/user-active/{userId}

Response 200:
{
  "status": "success",
  "count": 2,
  "activeParking": [...]
}
```

### 7. Get Current Occupancy
```http
GET /parking/occupancy

Response 200:
{
  "status": "success",
  "currentOccupancy": 42
}
```

### 8. Cancel Parking Ticket
```http
POST /parking/cancel?ticketReference=TKT-XXXX&reason=User%20request

Response 200:
{
  "status": "success",
  "message": "Ticket cancelled successfully",
  "ticket": {...}
}
```

---

## 💳 BILLING APIs

### 1. Generate Bill (Hourly Pricing)
```http
POST /billing/generate-hourly/{ticketId}

Response 201:
{
  "status": "success",
  "message": "Bill generated successfully",
  "bill": {
    "billId": 1,
    "invoiceNumber": "INV-1711025400000",
    "userId": 1,
    "ticketId": 1,
    "ratePerHour": 50.00,
    "durationHours": 2,
    "baseAmount": 100.00,
    "taxAmount": 10.00,
    "discountAmount": 0.00,
    "totalAmount": 110.00,
    "status": "PENDING"
  }
}
```

### 2. Generate Bill (Slab-Based Pricing)
```http
POST /billing/generate-slab/{ticketId}

Response 201:
{
  "status": "success",
  "message": "Bill generated successfully with slab-based pricing",
  "bill": {
    "billId": 2,
    "invoiceNumber": "INV-1711025410000",
    "baseAmount": 95.00,  // Reduced due to slab pricing
    "taxAmount": 9.50,
    "totalAmount": 104.50,
    "status": "PENDING",
    "notes": "Slab-based pricing applied"
  }
}
```

### 3. Get Bill by Invoice Number
```http
GET /billing/invoice/{invoiceNumber}

Response 200: Bill object
```

### 4. Get User's Bills
```http
GET /billing/user/{userId}

Response 200:
{
  "status": "success",
  "count": 15,
  "bills": [...]
}
```

### 5. Get User's Pending Bills
```http
GET /billing/user/{userId}/pending

Response 200:
{
  "status": "success",
  "pendingCount": 3,
  "bills": [...]
}
```

### 6. Mark Bill as Paid
```http
PUT /billing/{billId}/pay?paymentMethod=CARD

Response 200:
{
  "status": "success",
  "message": "Bill marked as paid successfully",
  "bill": {
    "billId": 1,
    "status": "PAID",
    "paymentMethod": "CARD",
    "paymentDate": "2026-03-19T15:50:00"
  }
}
```

### 7. Apply Discount
```http
PUT /billing/{billId}/apply-discount?discountAmount=10.00

Response 200:
{
  "status": "success",
  "message": "Discount applied successfully",
  "bill": {
    "billId": 1,
    "discountAmount": 10.00,
    "totalAmount": 100.00
  }
}
```

### 8. Get All Pending Bills (Admin)
```http
GET /billing/pending-all

Response 200:
{
  "status": "success",
  "totalPending": 25,
  "bills": [...]
}
```

### 9. Get Total Revenue (Admin)
```http
GET /billing/revenue-total

Response 200:
{
  "status": "success",
  "totalRevenue": 45680.50
}
```

---

## 🔌 Request/Response Examples

### Example 1: Complete Parking Flow

#### Step 1: Find nearest slot
```bash
curl -X GET "http://localhost:8080/api/slots/find-nearest?location=A1&slotType=NORMAL"
```

#### Step 2: Record vehicle entry
```bash
curl -X POST "http://localhost:8080/api/parking/entry" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "vehicleId": 1,
    "parkingSpaceId": 1
  }'
```

#### Step 3: Record vehicle exit (after parking)
```bash
curl -X POST "http://localhost:8080/api/parking/exit?ticketReference=TKT-A1B2C3D4E5F6"
```

#### Step 4: Generate bill
```bash
curl -X POST "http://localhost:8080/api/billing/generate-hourly/1"
```

#### Step 5: Mark as paid
```bash
curl -X PUT "http://localhost:8080/api/billing/1/pay?paymentMethod=CARD"
```

---

## 📊 Status Codes

| Code | Meaning |
|------|---------|
| 200 | OK - Request successful |
| 201 | Created - Resource created successfully |
| 400 | Bad Request - Invalid input |
| 404 | Not Found - Resource not found |
| 500 | Internal Server Error |

---

## 🔐 Error Responses

```json
{
  "status": "error",
  "message": "Descriptive error message"
}
```

---

## 📝 Notes

- All timestamps are in ISO 8601 format
- All monetary values use BigDecimal precision
- Vehicle types: CAR, BIKE, EV
- Slot types: NORMAL, VIP, HANDICAPPED
- Ticket statuses: ACTIVE, CLOSED, CANCELLED
- Billing statuses: PENDING, PAID, CANCELLED

