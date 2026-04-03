# 🚀 Quick Start Guide - Parking System

## 5 Minutes to First API Call

---

## 1️⃣ Database Setup (2 min)
```bash
# Login to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE parking_system_db;

# Run migration
psql -U postgres -d parking_system_db -f DATABASE_MIGRATION.sql
```

---

## 2️⃣ Configure Application (1 min)
Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/parking_system_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

---

## 3️⃣ Start Application (2 min)
```bash
cd "D:\Projects new(MS)\Github Repos"
mvn spring-boot:run
```

Wait for: **"Application started"** message

---

## 4️⃣ Test APIs (Using Postman or cURL)

### A. Register Vehicle
```bash
curl -X POST http://localhost:8080/api/vehicles/register \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "licensePlate": "KA-01-AB-1234",
    "vehicleType": "CAR",
    "vehicleModel": "Honda Civic",
    "vehicleColor": "Silver",
    "registrationNumber": "REG-001"
  }'
```
**Response:** Vehicle ID (save it)

---

### B. Find Available Parking Slot
```bash
curl -X GET "http://localhost:8080/api/slots/find-nearest?location=A1&slotType=NORMAL"
```
**Response:** Parking space ID (save it)

---

### C. Record Vehicle Entry
```bash
curl -X POST http://localhost:8080/api/parking/entry \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "vehicleId": 1,
    "parkingSpaceId": 1
  }'
```
**Response:** Ticket reference (save it - e.g., TKT-XXXXX)

---

### D. Record Vehicle Exit (after a few minutes)
```bash
curl -X POST "http://localhost:8080/api/parking/exit?ticketReference=TKT-XXXXX"
```
**Response:** Exit recorded with duration

---

### E. Generate Billing Invoice
```bash
curl -X POST http://localhost:8080/api/billing/generate-hourly/1
```
**Response:** Invoice with amount due

---

### F. Check Real-Time Availability
```bash
curl -X GET http://localhost:8080/api/slots/availability-summary
```
**Response:**
```json
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

---

## ✅ Verification Checklist

- [x] Database created and tables populated
- [x] Application running on port 8080
- [x] Vehicle registered successfully
- [x] Parking slot found
- [x] Entry recorded (received ticket reference)
- [x] Exit recorded (duration calculated)
- [x] Bill generated
- [x] Real-time availability tracked

---

## 🎯 What's Now Available

### Core Features:
✅ Vehicle Management (CAR, BIKE, EV)
✅ Intelligent Slot Allocation (NORMAL, VIP, HANDICAPPED)
✅ Entry/Exit Tracking with Auto-Generated Tickets
✅ Hourly & Slab-Based Billing
✅ Real-Time Availability Dashboard

### API Endpoints (9 Controllers):
- **VehicleController** (6 endpoints)
- **SlotAllocationController** (7 endpoints)
- **ParkingEntryExitController** (8 endpoints)
- **BillingController** (9 endpoints)
- Plus existing: AuthController, UserController, FeedbackController, ReservationController, ParkingSpaceController

---

## 📊 Key Files Created

### Models (4)
- Vehicle.java
- ParkingTicket.java
- Bill.java
- ParkingSpace.java (enhanced)

### Services (5)
- VehicleService.java
- ParkingEntryExitService.java
- SlotAllocationService.java
- BillingService.java

### Controllers (4)
- VehicleController.java
- ParkingEntryExitController.java
- SlotAllocationController.java
- BillingController.java

### Repositories (4)
- VehicleRepository.java
- ParkingTicketRepository.java
- BillRepository.java
- ParkingSpaceRepository.java (enhanced)

### DTOs (6)
- VehicleRegistrationRequest.java
- VehicleDTO.java
- VehicleEntryRequest.java
- ParkingTicketDTO.java
- BillDTO.java
- SlotAvailabilitySummaryDTO.java

### Enums (4)
- VehicleType.java
- SlotType.java
- TicketStatus.java
- BillingStatus.java

---

## 📚 Documentation

- **API_DOCUMENTATION.md** - All 30+ endpoints with examples
- **SETUP_AND_CONFIG_GUIDE.md** - Complete setup instructions
- **IMPLEMENTATION_SUMMARY.md** - Feature details
- **DATABASE_MIGRATION.sql** - Database schema & views
- **FEATURE_CHECKLIST.md** - Status of all features

---

## 🔧 Common Issues

| Issue | Solution |
|-------|----------|
| Connection refused | Start PostgreSQL service |
| Table doesn't exist | Run DATABASE_MIGRATION.sql |
| Port 8080 already in use | Change server.port in properties |
| FK constraint error | Ensure parking_user table exists |
| Empty response | Check logs for exceptions |

---

## 🚀 Next Steps

### Phase 2 - Add These Features:
1. **Swagger/OpenAPI** - API documentation UI
2. **Payment Integration** - Stripe/PayPal
3. **Email Notifications** - Reservation confirmations
4. **Admin Dashboard** - Analytics & reporting
5. **Mobile App API** - Push notifications

### Phase 3 - Production Ready:
1. Add comprehensive tests
2. Enable caching (Redis)
3. Set up CI/CD pipeline
4. Database backups
5. Performance monitoring

---

## 💡 Pro Tips

1. **Use Postman Collection:** Import all endpoints at once
2. **Monitor Database:** Use `SELECT * FROM parking_dashboard;`
3. **Track Revenue:** Use `SELECT * FROM revenue_report;`
4. **Debug Queries:** Enable `spring.jpa.show-sql=true`
5. **Performance:** Check `parking_tickets` indexes regularly

---

## 🎓 Learning Path

**If you want to extend this:**
1. Add WebSocket for real-time updates
2. Implement caching for slot availability
3. Create a React/Angular frontend
4. Add mobile app (React Native)
5. Deploy to cloud (AWS/GCP/Azure)

---

**Congratulations! Your parking system is up and running!** 🎉

