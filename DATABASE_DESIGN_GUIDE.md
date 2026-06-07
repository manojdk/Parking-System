# Database Design & Optimization Guide

**Version:** 1.0  
**Last Updated:** June 7, 2026  
**Target Audience:** Database administrators, backend developers  

---

## Table of Contents

1. [Database Overview](#database-overview)
2. [Schema Design](#schema-design)
3. [Data Types & Constraints](#data-types--constraints)
4. [Indexing Strategy](#indexing-strategy)
5. [Query Optimization](#query-optimization)
6. [Backup & Recovery](#backup--recovery)
7. [Monitoring & Performance](#monitoring--performance)
8. [Troubleshooting](#troubleshooting)

---

## Database Overview

### System Architecture

```
┌─────────────────────────────────────┐
│     Application Layer               │
│  (Spring Boot with JPA/Hibernate)   │
└──────────────┬──────────────────────┘
               │
        HikariCP Connection Pool
        (10-20 connections)
               │
┌──────────────▼──────────────────────┐
│   PostgreSQL Database Server         │
│   - Version: 12+                     │
│   - Encoding: UTF-8                  │
│   - Max Connections: 100-200         │
└──────────────┬──────────────────────┘
               │
        ┌──────┴──────┐
        │             │
   PRIMARY DB    REPLICA DB (Optional)
   (Master)      (Read-Only)
```

### Database Characteristics

```
Database Name:      parking_system_db
Owner/Role:         parking_admin
Encoding:          UTF-8
Locale:            en_US.UTF-8
Data Size (Estimate): 500MB - 2GB (depending on volume)
```

---

## Schema Design

### Complete Entity Relationship Diagram (ERD)

```
parking_user (Users Table)
├─ user_id (PK): BIGSERIAL
├─ user_name: VARCHAR(100) UNIQUE
├─ user_email: VARCHAR(100) UNIQUE
├─ password: VARCHAR(255)
├─ role: VARCHAR(50) DEFAULT 'USER'
├─ license_plate: VARCHAR(50)
├─ registration_date: DATE
├─ created_date: TIMESTAMP
└─ updated_date: TIMESTAMP
     │
     ├──→ vehicles (1:N)
     ├──→ parking_tickets (1:N)
     ├──→ bills (1:N)
     ├──→ reservations (1:N)
     └──→ feedback (1:N)

vehicles (Vehicles Table)
├─ vehicle_id (PK): BIGSERIAL
├─ user_id (FK): BIGINT → parking_user
├─ license_plate: VARCHAR(50) UNIQUE
├─ vehicle_type: vehicle_type_enum (CAR, BIKE, EV)
├─ vehicle_model: VARCHAR(100)
├─ vehicle_color: VARCHAR(50)
├─ registration_number: VARCHAR(100) UNIQUE
├─ is_active: BOOLEAN DEFAULT TRUE
├─ created_at: TIMESTAMP
├─ updated_at: TIMESTAMP
└─ created_at (timestamps should never be updated)

parking_spaces (Parking Spaces Table)
├─ parking_space_id (PK): BIGSERIAL
├─ space_number: VARCHAR(50) UNIQUE
├─ location: VARCHAR(100)
├─ floor_number: INTEGER
├─ slot_type: slot_type_enum (NORMAL, VIP, HANDICAPPED)
├─ availability_status: VARCHAR(50)
├─ rate_per_hour: NUMERIC(10,2)
├─ notes: TEXT
├─ is_active: BOOLEAN DEFAULT TRUE
├─ created_at: TIMESTAMP
├─ updated_at: TIMESTAMP
└─ updated_at (tracks last modification)

parking_tickets (Parking Sessions Table)
├─ ticket_id (PK): BIGSERIAL
├─ ticket_reference: VARCHAR(100) UNIQUE
├─ user_id (FK): BIGINT → parking_user
├─ vehicle_id (FK): BIGINT → vehicles
├─ parking_space_id (FK): BIGINT → parking_spaces
├─ entry_time: TIMESTAMP
├─ exit_time: TIMESTAMP (nullable - NULL if still parked)
├─ duration_minutes: BIGINT (nullable)
├─ status: ticket_status_enum (ACTIVE, CLOSED, CANCELLED)
├─ entry_photo_url: VARCHAR(500)
├─ exit_photo_url: VARCHAR(500)
├─ notes: TEXT
├─ created_at: TIMESTAMP
└─ created_at (immutable - entry time)

bills (Billing Records Table)
├─ bill_id (PK): BIGSERIAL
├─ invoice_number: VARCHAR(100) UNIQUE
├─ user_id (FK): BIGINT → parking_user
├─ ticket_id (FK): BIGINT → parking_tickets (UNIQUE)
├─ rate_per_hour: NUMERIC(10,2)
├─ duration_hours: NUMERIC(10,2)
├─ base_amount: NUMERIC(10,2)
├─ tax_amount: NUMERIC(10,2) DEFAULT 0.00
├─ discount_amount: NUMERIC(10,2) DEFAULT 0.00
├─ total_amount: NUMERIC(10,2)
├─ status: billing_status_enum (PENDING, PAID, CANCELLED)
├─ payment_method: VARCHAR(50)
├─ payment_date: TIMESTAMP
├─ billing_date: TIMESTAMP
├─ notes: TEXT
└─ created_at: TIMESTAMP

reservations (Future Reservations Table)
├─ reservation_id (PK): BIGSERIAL
├─ user_id (FK): BIGINT → parking_user
├─ parking_space_id (FK): BIGINT → parking_spaces
├─ reservation_date: DATE
├─ desired_time: TIME
├─ status: VARCHAR(50) (CONFIRMED, CANCELLED, COMPLETED)
├─ created_at: TIMESTAMP
└─ updated_at: TIMESTAMP

feedback (User Feedback Table)
├─ feedback_id (PK): BIGSERIAL
├─ user_id (FK): BIGINT → parking_user
├─ ticket_id (FK): BIGINT → parking_tickets (nullable)
├─ rating: SMALLINT (1-5)
├─ comment: TEXT
├─ created_at: TIMESTAMP
└─ updated_at: TIMESTAMP
```

---

## Data Types & Constraints

### Recommended Data Types

```sql
-- Numeric Types
BIGSERIAL          -- Auto-incrementing 64-bit integer (PK)
SERIAL             -- Auto-incrementing 32-bit integer
NUMERIC(10,2)      -- Fixed precision decimal (for money)
BIGINT             -- 64-bit integer
SMALLINT           -- 16-bit integer (ratings, counts)

-- Date/Time Types
TIMESTAMP          -- Date and time with precision
DATE               -- Date only
TIME               -- Time only

-- Text Types
VARCHAR(n)         -- Variable length string
TEXT               -- Unlimited length string (avoid for PK/FK)
CHAR(n)            -- Fixed length string

-- Boolean
BOOLEAN            -- TRUE/FALSE

-- Enumerated Types
CREATE TYPE vehicle_type_enum AS ENUM ('CAR', 'BIKE', 'EV');
CREATE TYPE slot_type_enum AS ENUM ('NORMAL', 'VIP', 'HANDICAPPED');
CREATE TYPE ticket_status_enum AS ENUM ('ACTIVE', 'CLOSED', 'CANCELLED');
CREATE TYPE billing_status_enum AS ENUM ('PENDING', 'PAID', 'CANCELLED');
```

### Constraint Guidelines

```sql
-- Primary Key Constraint
PRIMARY KEY (column)              -- Unique identifier for each row
  └─ Automatically creates index

-- Foreign Key Constraint
FOREIGN KEY (column) REFERENCES parent_table(pk_column)
  └─ Ensures referential integrity
  └─ ON DELETE CASCADE - Delete child records when parent deleted
  └─ ON DELETE SET NULL - Set child FK to NULL when parent deleted

-- Unique Constraint
UNIQUE (column)                   -- Ensures no duplicate values
  └─ Automatically creates index
  └─ Example: email addresses, license plates

-- NOT NULL Constraint
NOT NULL                          -- Column must have value
  └─ Use for mandatory fields

-- CHECK Constraint
CHECK (column > 0)                -- Ensure value meets condition
  └─ Example: rate_per_hour > 0
  └─ Example: rating BETWEEN 1 AND 5

-- Default Values
DEFAULT value                     -- Use when value not provided
  └─ Example: is_active DEFAULT TRUE
  └─ Example: created_at DEFAULT CURRENT_TIMESTAMP
```

### Practical Examples

```sql
-- User Entity
CREATE TABLE parking_user (
    user_id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(100) UNIQUE NOT NULL,
    user_email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'USER'
        CHECK (role IN ('USER', 'ADMIN', 'MODERATOR')),
    license_plate VARCHAR(50) NOT NULL,
    registration_date DATE,
    created_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_date TIMESTAMP
);

-- Vehicle Entity
CREATE TABLE vehicles (
    vehicle_id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES parking_user(user_id) 
        ON DELETE CASCADE,
    license_plate VARCHAR(50) UNIQUE NOT NULL,
    vehicle_type vehicle_type_enum NOT NULL,
    vehicle_model VARCHAR(100),
    vehicle_color VARCHAR(50),
    registration_number VARCHAR(100) UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CHECK (license_plate ~ '^[A-Z]{2}-[0-9]{2}-[A-Z]{2}-[0-9]{4}$')
);
```

---

## Indexing Strategy

### Core Indexing Plan

```sql
-- 1. PRIMARY KEY INDEXES (Automatic)
-- These are created automatically
-- user_id, vehicle_id, etc.

-- 2. FOREIGN KEY INDEXES
-- Improve join performance
CREATE INDEX idx_vehicle_user_id ON vehicles(user_id);
CREATE INDEX idx_parking_ticket_user_id ON parking_tickets(user_id);
CREATE INDEX idx_parking_ticket_vehicle_id ON parking_tickets(vehicle_id);
CREATE INDEX idx_parking_ticket_space_id ON parking_tickets(parking_space_id);
CREATE INDEX idx_bill_user_id ON bills(user_id);
CREATE INDEX idx_bill_ticket_id ON bills(ticket_id);

-- 3. FREQUENTLY SEARCHED COLUMNS
-- Email and username for authentication
CREATE INDEX idx_user_email ON parking_user(user_email);
CREATE INDEX idx_user_username ON parking_user(user_name);

-- License plate for vehicle lookups
CREATE INDEX idx_vehicle_license_plate ON vehicles(license_plate);

-- 4. STATUS/STATE COLUMNS
-- Used heavily in WHERE clauses
CREATE INDEX idx_parking_space_availability 
    ON parking_spaces(availability_status);
CREATE INDEX idx_parking_ticket_status ON parking_tickets(status);
CREATE INDEX idx_bill_status ON bills(status);

-- 5. COMPOSITE INDEXES
-- For frequently used filter combinations
CREATE INDEX idx_bill_user_status 
    ON bills(user_id, status);
CREATE INDEX idx_bill_status_billing_date 
    ON bills(status, billing_date DESC);
CREATE INDEX idx_parking_ticket_user_status 
    ON parking_tickets(user_id, status);

-- 6. TIME-BASED INDEXES
-- For date range queries
CREATE INDEX idx_parking_ticket_entry_time 
    ON parking_tickets(entry_time DESC);
CREATE INDEX idx_parking_ticket_status_exit_time 
    ON parking_tickets(status, exit_time);
CREATE INDEX idx_bill_billing_date 
    ON bills(billing_date DESC);

-- 7. UNIQUE INDEXES
-- For unique constraints
CREATE UNIQUE INDEX idx_vehicle_license_plate_unique 
    ON vehicles(license_plate);
CREATE UNIQUE INDEX idx_bill_invoice_number_unique 
    ON bills(invoice_number);
CREATE UNIQUE INDEX idx_parking_ticket_reference_unique 
    ON parking_tickets(ticket_reference);

-- 8. PARTIAL INDEXES
-- For frequently filtered subsets
CREATE INDEX idx_active_vehicles 
    ON vehicles(user_id) WHERE is_active = TRUE;
CREATE INDEX idx_active_parking_tickets 
    ON parking_tickets(user_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_available_parking_spaces 
    ON parking_spaces(location) WHERE availability_status = 'available';
```

### Index Maintenance

```sql
-- Check index fragmentation
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Reindex fragmented indexes
REINDEX INDEX CONCURRENTLY idx_parking_ticket_entry_time;

-- Analyze table statistics
ANALYZE parking_tickets;

-- Vacuum to recover space
VACUUM ANALYZE parking_spaces;

-- Check index size
SELECT schemaname, tablename, indexname, 
       pg_size_pretty(pg_relation_size(indexrelid)) AS size
FROM pg_stat_user_indexes
ORDER BY pg_relation_size(indexrelid) DESC;
```

---

## Query Optimization

### N+1 Query Problem & Solutions

```sql
-- ❌ BAD: Causes N+1 query problem
SELECT u.user_id, u.user_name FROM parking_user u;
-- Then a separate query for each user:
SELECT v.* FROM vehicles WHERE user_id = ?;

-- ✓ GOOD: Use LEFT JOIN FETCH
SELECT u, v FROM parking_user u 
LEFT JOIN FETCH u.vehicles v
WHERE u.is_active = TRUE;

-- Result: Single query with all data
```

### Execution Plan Analysis

```sql
-- Analyze query performance
EXPLAIN ANALYZE
SELECT pt.ticket_id, pt.ticket_reference, u.user_name, v.license_plate
FROM parking_tickets pt
JOIN parking_user u ON pt.user_id = u.user_id
JOIN vehicles v ON pt.vehicle_id = v.vehicle_id
WHERE pt.status = 'CLOSED'
AND pt.billing_date >= '2026-06-01'
ORDER BY pt.ticket_id DESC
LIMIT 100;

-- Output shows:
-- - Index usage (good) vs Sequential Scans (bad)
-- - Actual vs Estimated rows
-- - Query time
```

### Common Query Patterns

```sql
-- 1. GET AVAILABLE SPACES
SELECT * FROM parking_spaces
WHERE availability_status = 'available'
AND slot_type = 'NORMAL'
AND is_active = TRUE
ORDER BY rate_per_hour ASC
LIMIT 10;
-- Uses: idx_parking_space_availability, idx_parking_spaces_slot_type

-- 2. GET USER PARKING HISTORY
SELECT pt.*, u.user_name, v.license_plate, ps.space_number
FROM parking_tickets pt
JOIN parking_user u ON pt.user_id = u.user_id
JOIN vehicles v ON pt.vehicle_id = v.vehicle_id
JOIN parking_spaces ps ON pt.parking_space_id = ps.parking_space_id
WHERE pt.user_id = $1
AND pt.status = 'CLOSED'
ORDER BY pt.entry_time DESC
LIMIT 50;
-- Uses: idx_parking_ticket_user_status

-- 3. GET DAILY REVENUE
SELECT DATE(billing_date) as billing_date,
       COUNT(*) as total_transactions,
       SUM(CASE WHEN status = 'PAID' THEN total_amount ELSE 0 END) as daily_revenue,
       AVG(total_amount) as avg_bill_amount
FROM bills
WHERE billing_date >= $1 AND billing_date < $2
GROUP BY DATE(billing_date)
ORDER BY billing_date DESC;
-- Uses: idx_bill_billing_date

-- 4. GET ACTIVE PARKING SESSIONS
SELECT pt.*, v.license_plate, ps.space_number,
       EXTRACT(EPOCH FROM (NOW() - pt.entry_time))/60 as duration_minutes
FROM parking_tickets pt
JOIN vehicles v ON pt.vehicle_id = v.vehicle_id
JOIN parking_spaces ps ON pt.parking_space_id = ps.parking_space_id
WHERE pt.status = 'ACTIVE'
order by pt.entry_time ASC;
-- Uses: idx_active_parking_tickets
```

---

## Backup & Recovery

### Backup Strategy

```
BACKUP FREQUENCY:
├─ Full Backup:     Weekly (Sunday 2 AM UTC)
├─ Incremental:     Daily (1 AM UTC)
└─ Transaction Log: Every 15 minutes

RETENTION POLICY:
├─ Full Backups:    Keep 4 weeks
├─ Incremental:     Keep 7 days
└─ Transaction Log: Keep 24 hours
```

### Backup Commands

```bash
# Full Database Backup
pg_dump --format=custom parking_system_db > parking-full-$(date +%Y%m%d-%H%M%S).dump

# Backup with compression
pg_dump --format=custom --file=parking-full.dump --compress=9 parking_system_db

# Backup specific table
pg_dump --format=custom --table=parking_tickets parking_system_db > tickets.dump

# Remote backup to S3
pg_dump parking_system_db | gzip | aws s3 cp - s3://backup-bucket/parking-system/$(date +%Y%m%d).dump.gz

# List backup files
ls -lh parking-full-*.dump
```

### Recovery Procedures

```bash
# Restore from backup
pg_restore --dbname=parking_system_db parking-full-20260607.dump

# Restore specific table
pg_restore --dbname=parking_system_db --table=bills parking-full.dump

# Restore to different database (for testing)
createdb parking_system_db_test
pg_restore --dbname=parking_system_db_test parking-full.dump

# List backup contents
pg_restore --list parking-full.dump | head -20
```

### Point-in-Time Recovery (PITR)

```bash
# Enable WAL archiving in postgresql.conf
wal_level = replica
max_wal_senders = 3
wal_keep_size = 1GB

# Restore to specific timestamp
pg_restore --target-xid=123456789 parking_system_db

# Restore to specific timeline
pg_restore --target-timeline=1 parking_system_db
```

---

## Monitoring & Performance

### Key Metrics to Monitor

```sql
-- Connection count
SELECT count(*) FROM pg_stat_activity;

-- Long-running queries (> 5 minutes)
SELECT pid, usename, application_name, 
       NOW() - query_start as duration,
       query
FROM pg_stat_activity
WHERE NOW() - query_start > interval '5 minutes';

-- Cache hit ratio (should be > 99%)
SELECT sum(heap_blks_hit) / (sum(heap_blks_hit) + sum(heap_blks_read)) * 100 
as cache_hit_ratio
FROM pg_statio_user_tables;

-- Table sizes
SELECT schemaname, tablename, 
       pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Index usage statistics
SELECT schemaname, tablename, indexname, idx_scan, idx_tup_read, idx_tup_fetch
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- Bloat (Dead tuples)
SELECT schemaname, tablename, 
       ROUND(100 * LIVE_TUPLES / (LIVE_TUPLES+DEAD_TUPLES), 2) as live_ratio
FROM pg_stat_user_tables
WHERE LIVE_TUPLES + DEAD_TUPLES > 0
ORDER BY live_ratio ASC;
```

### Performance Tuning Parameters

```sql
-- postgresql.conf settings

-- Memory
shared_buffers = 256MB              # 25% of RAM (min 128MB)
effective_cache_size = 4GB          # Available RAM
work_mem = 16MB                     # Per operation sort/hash space
maintenance_work_mem = 64MB         # For maintenance operations

-- Parallelization
max_parallel_workers_per_gather = 2
max_parallel_workers = 4
max_parallel_maintenance_workers = 2

-- Query Planning
random_page_cost = 1.1              # For SSD (vs 4.0 for HDD)
effective_io_concurrency = 200      # For SSD

-- Logging
log_min_duration_statement = 1000   # Log queries > 1 second
log_statement = 'mod'               # Log modifications
log_checkpoints = on
log_autovacuum_min_duration = 0
```

---

## Troubleshooting

### Common Issues & Solutions

```
ISSUE 1: Slow Queries
├─ Check: SELECT * FROM pg_stat_statements
├─ Analyze: EXPLAIN ANALYZE
├─ Fix: Add/optimize indexes, rewrite query
└─ Monitor: Enable slow_query_log

ISSUE 2: High Connection Count
├─ Check: SELECT count(*) FROM pg_stat_activity
├─ Causes: Connection leaks, insufficient pool size
├─ Fix: Restart stale connections, increase pool
└─ Limit: max_connections parameter

ISSUE 3: Blocked Transactions
├─ Check: SELECT * FROM pg_locks WHERE NOT granted
├─ Identify: Find blocking query
├─ Fix: CANCEL/ROLLBACK blocking query
└─ Script: Use pg_terminate_backend(pid)

ISSUE 4: Out of Memory
├─ Check: free -h (server) or Task Manager (Windows)
├─ Causes: Large queries, insufficient swap
├─ Fix: Reduce work_mem, add swap, upgrade hardware
└─ Monitor: Watch for OOM killer activity

ISSUE 5: Disk Space Issues
├─ Check: df -h / (Unix) or Get-Volume (PowerShell)
├─ Causes: Log files, WAL files, full backups
├─ Fix: Clear old logs, archive backups, extend fs
└─ Solution: Move pg_log to separate volume
```

### Connection Troubleshooting

```bash
# Test connection
psql -U parking_admin -d parking_system_db -c "SELECT 1"

# Check connection limit
psql -U postgres -c "SELECT * FROM pg_settings WHERE name = 'max_connections'"

# Monitor active connections
watch -n 1 "psql -U postgres -c 'SELECT count(*) FROM pg_stat_activity'"

# Kill idle connections (careful!)
psql -U postgres -c "
SELECT pg_terminate_backend(pid) 
FROM pg_stat_activity 
WHERE datname = 'parking_system_db' 
AND state = 'idle'
AND state_change < now() - interval '10 minutes'"
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-06-07 | Initial database design guide |

---

**Document Prepared By:** Database Team  
**Last Updated:** 2026-06-07  
**Next Review:** 2026-09-07

**End of Document**

