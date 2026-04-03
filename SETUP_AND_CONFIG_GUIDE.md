# 🚗 Parking System - Setup & Configuration Guide

## 📋 Prerequisites
- Java 17+
- PostgreSQL 12+
- Maven 3.6+
- Spring Boot 3.3.1

---

## 🔧 Step 1: Database Setup

### 1.1 Create Database
```sql
CREATE DATABASE parking_system_db;
```

### 1.2 Create User
```sql
CREATE USER parking_user WITH PASSWORD 'Manoj@123';
GRANT ALL PRIVILEGES ON DATABASE parking_system_db TO parking_user;
```

### 1.3 Run Migration Scripts
Execute the `DATABASE_MIGRATION.sql` script in your PostgreSQL client:
```bash
psql -U postgres -d parking_system_db -f DATABASE_MIGRATION.sql
```

---

## ⚙️ Step 2: Application Configuration

### 2.1 Update application.properties
File: `src/main/resources/application.properties`

```properties
# Application
spring.application.name=Parking-System

# PostgreSQL Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/parking_system_db
spring.datasource.username=parking_user
spring.datasource.password=Manoj@123
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.batch_size=10
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# JSON Serialization
spring.jackson.serialization.write-dates-as-timestamps=false
spring.jackson.default-property-inclusion=non_null

# Logging
logging.level.root=INFO
logging.level.com.parkingSystem=DEBUG
logging.level.org.springframework.security=DEBUG

# Server
server.port=8080
server.servlet.context-path=/

# Timezone
spring.jackson.time-zone=UTC
```

### 2.2 Create application-dev.properties (Development)
```properties
# Development Profile
spring.datasource.url=jdbc:postgresql://localhost:5432/parking_system_db
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.show-sql=true
logging.level.com.parkingSystem=DEBUG
```

### 2.3 Create application-prod.properties (Production)
```properties
# Production Profile - Use Environment Variables
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.show-sql=false
logging.level.com.parkingSystem=INFO
```

---

## 📦 Step 3: Update pom.xml Dependencies

The following dependencies are already included. If not, add them:

```xml
<!-- Existing dependencies are complete -->
<!-- All required libraries are in pom.xml -->
```

---

## 🚀 Step 4: Build & Run

### 4.1 Build Project
```bash
cd "D:\Projects new(MS)\Github Repos"
mvn clean install
```

### 4.2 Run Application
```bash
mvn spring-boot:run
```

Or use your IDE to run: `ParkingSystemApplication.java`

### 4.3 Access Application
- **API Base URL:** http://localhost:8080/api
- **Health Check:** http://localhost:8080/api/parking/occupancy

---

## 🧪 Step 5: Initial Testing

### Test 1: Register a Vehicle
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

### Test 2: Get Availability Summary
```bash
curl -X GET http://localhost:8080/api/slots/availability-summary
```

### Test 3: Find Nearest Slot
```bash
curl -X GET "http://localhost:8080/api/slots/find-nearest?location=A1&slotType=NORMAL"
```

### Test 4: Record Vehicle Entry
```bash
curl -X POST http://localhost:8080/api/parking/entry \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "vehicleId": 1,
    "parkingSpaceId": 1
  }'
```

### Test 5: Record Vehicle Exit
```bash
curl -X POST "http://localhost:8080/api/parking/exit?ticketReference=TKT-XXXXXXXX"
```

### Test 6: Generate Bill
```bash
curl -X POST http://localhost:8080/api/billing/generate-hourly/1
```

---

## 📝 Step 6: Additional Configuration (Optional)

### 6.1 Enable CORS (if frontend is separate)
Already configured in `CorsConfig.java`

### 6.2 JWT Configuration
- Secret Key Location: `JwtUtil.java` (line 13)
- Expiration: 24 hours (86400000 ms)
- ⚠️ **IMPORTANT:** Change secret key in production!

```java
private static final String SECRET_KEY = "your-secret-key-here-should-be-at-least-256-bits-long";
```

### 6.3 Enable Swagger/OpenAPI (Future Enhancement)
Add to pom.xml:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.0.2</version>
</dependency>
```

---

## 🔍 Step 7: Verify Installation

### Check Database Connection
```bash
# In application logs, look for:
# "Hibernate: select 1"
# "HikariPool initialized"
```

### Check Created Tables
```sql
SELECT table_name FROM information_schema.tables 
WHERE table_schema = 'public' 
ORDER BY table_name;
```

Expected tables:
- parking_user
- parking_spaces
- vehicles
- parking_tickets
- bills
- feedback
- parking_reservations

### Check Views
```sql
SELECT viewname FROM pg_views 
WHERE schemaname = 'public';
```

Expected views:
- parking_dashboard
- revenue_report
- user_parking_stats

---

## 🐛 Troubleshooting

### Issue 1: Database Connection Failed
**Solution:**
- Check PostgreSQL is running
- Verify credentials in application.properties
- Ensure parking_system_db exists
- Verify user has proper permissions

### Issue 2: Table Already Exists
**Solution:**
- Drop and recreate database
- Or use spring.jpa.hibernate.ddl-auto=create

### Issue 3: FK Constraint Violations
**Solution:**
- Ensure parking_user table exists first
- Run DATABASE_MIGRATION.sql in order

### Issue 4: UUID Generation Issues
**Solution:**
- Ensure `@PrePersist` methods are being called
- Check ticket_reference is being populated

### Issue 5: Token/Authentication Issues
**Solution:**
- Generate new JWT token via /api/auth/login
- Ensure SecurityConfig is properly loaded
- Check @Component annotation on JwtUtil

---

## 📊 Monitoring & Debugging

### 1. Check Active Parkings
```sql
SELECT * FROM parking_dashboard;
```

### 2. Check Recent Bills
```sql
SELECT * FROM bills ORDER BY created_at DESC LIMIT 10;
```

### 3. Check User Statistics
```sql
SELECT * FROM user_parking_stats;
```

### 4. Calculate Current Occupancy
```sql
SELECT get_occupancy_percentage();
```

### 5. View Application Logs
```bash
# For Spring Boot
# Logs will be printed to console and optionally to file
# Configure in application.properties:
logging.file.name=logs/app.log
logging.file.max-size=10MB
logging.file.max-history=10
```

---

## 🔐 Security Checklist

- [ ] Change JWT secret key
- [ ] Use environment variables for credentials
- [ ] Enable HTTPS in production
- [ ] Configure rate limiting
- [ ] Add input validation
- [ ] Enable CSRF protection for web forms
- [ ] Use strong database passwords
- [ ] Implement API authentication
- [ ] Add request/response logging
- [ ] Monitor for suspicious activities

---

## 📈 Performance Optimization

### 1. Enable Query Caching
```properties
spring.jpa.properties.hibernate.cache.use_second_level_cache=true
spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
```

### 2. Connection Pooling
```properties
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
```

### 3. Batch Processing
Already configured in application.properties

### 4. Lazy Loading
Already implemented in models with `FetchType.LAZY`

---

## 🚀 Deployment

### Docker (Optional)
Create `Dockerfile`:
```dockerfile
FROM openjdk:17-slim
COPY target/Parking-System-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
```

### Cloud Deployment
- AWS: Deploy to EC2 or ECS
- Azure: Deploy to App Service
- GCP: Deploy to Cloud Run
- Heroku: Deploy using Git

---

## 📚 Documentation Files

- **API_DOCUMENTATION.md** - All API endpoints
- **IMPLEMENTATION_SUMMARY.md** - Feature overview
- **DATABASE_MIGRATION.sql** - Database schema
- **PROJECT_ANALYSIS.md** - Project roadmap
- **FEATURE_CHECKLIST.md** - Feature status

---

## 🆘 Support & Contact

For issues or questions:
1. Check troubleshooting section
2. Review logs in `logs/` directory
3. Verify database connection
4. Check application.properties configuration

---

**Setup Complete! Your Parking System is ready to use.** ✅

