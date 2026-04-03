-- =====================================================
-- Parking System Database Migration SQL
-- PostgreSQL Script for Core Features
-- =====================================================

-- =====================================================
-- 1. Create ENUM Types
-- =====================================================

CREATE TYPE vehicle_type_enum AS ENUM ('CAR', 'BIKE', 'EV');
CREATE TYPE slot_type_enum AS ENUM ('NORMAL', 'VIP', 'HANDICAPPED');
CREATE TYPE ticket_status_enum AS ENUM ('ACTIVE', 'CLOSED', 'CANCELLED');
CREATE TYPE billing_status_enum AS ENUM ('PENDING', 'PAID', 'CANCELLED');

-- =====================================================
-- 2. Create Vehicles Table
-- =====================================================

CREATE TABLE vehicles (
    vehicle_id SERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    license_plate VARCHAR(50) NOT NULL UNIQUE,
    vehicle_type vehicle_type_enum NOT NULL,
    vehicle_model VARCHAR(100),
    vehicle_color VARCHAR(50),
    registration_number VARCHAR(100) UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (user_id) REFERENCES parking_user(user_id) ON DELETE CASCADE
);

CREATE INDEX idx_vehicles_user_id ON vehicles(user_id);
CREATE INDEX idx_vehicles_license_plate ON vehicles(license_plate);
CREATE INDEX idx_vehicles_is_active ON vehicles(is_active);

-- =====================================================
-- 3. Update Parking Spaces Table
-- =====================================================

ALTER TABLE parking_spaces
    ADD COLUMN space_number VARCHAR(50) UNIQUE,
    ADD COLUMN floor_number INTEGER,
    ADD COLUMN slot_type slot_type_enum NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN rate_per_hour DECIMAL(10, 2),
    ADD COLUMN notes TEXT,
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMP;

-- Migrate old 'rate' column to 'rate_per_hour'
UPDATE parking_spaces
SET rate_per_hour = CAST(rate AS DECIMAL(10, 2))
WHERE rate_per_hour IS NULL;

-- Drop old type column if it exists
ALTER TABLE parking_spaces DROP COLUMN IF EXISTS type CASCADE;
ALTER TABLE parking_spaces DROP COLUMN IF EXISTS rate CASCADE;

CREATE INDEX idx_parking_spaces_slot_type ON parking_spaces(slot_type);
CREATE INDEX idx_parking_spaces_availability ON parking_spaces(availability_status);
CREATE INDEX idx_parking_spaces_location ON parking_spaces(location);
CREATE INDEX idx_parking_spaces_floor ON parking_spaces(floor_number);

-- =====================================================
-- 4. Create Parking Tickets Table
-- =====================================================

CREATE TABLE parking_tickets (
    ticket_id SERIAL PRIMARY KEY,
    ticket_reference VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    vehicle_id BIGINT NOT NULL,
    parking_space_id BIGINT NOT NULL,
    entry_time TIMESTAMP NOT NULL,
    exit_time TIMESTAMP,
    duration_minutes BIGINT,
    status ticket_status_enum NOT NULL DEFAULT 'ACTIVE',
    entry_photo_url VARCHAR(500),
    exit_photo_url VARCHAR(500),
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES parking_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicles(vehicle_id) ON DELETE CASCADE,
    FOREIGN KEY (parking_space_id) REFERENCES parking_spaces(parking_space_id) ON DELETE CASCADE
);

CREATE INDEX idx_parking_tickets_reference ON parking_tickets(ticket_reference);
CREATE INDEX idx_parking_tickets_user_id ON parking_tickets(user_id);
CREATE INDEX idx_parking_tickets_vehicle_id ON parking_tickets(vehicle_id);
CREATE INDEX idx_parking_tickets_parking_space_id ON parking_tickets(parking_space_id);
CREATE INDEX idx_parking_tickets_status ON parking_tickets(status);
CREATE INDEX idx_parking_tickets_entry_time ON parking_tickets(entry_time);
CREATE INDEX idx_parking_tickets_status_exit_time ON parking_tickets(status, exit_time);

-- =====================================================
-- 5. Create Bills Table
-- =====================================================

CREATE TABLE bills (
    bill_id SERIAL PRIMARY KEY,
    invoice_number VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL UNIQUE,
    rate_per_hour DECIMAL(10, 2) NOT NULL,
    duration_hours DECIMAL(10, 2) NOT NULL,
    base_amount DECIMAL(10, 2) NOT NULL,
    tax_amount DECIMAL(10, 2) DEFAULT 0.00,
    discount_amount DECIMAL(10, 2) DEFAULT 0.00,
    total_amount DECIMAL(10, 2) NOT NULL,
    status billing_status_enum NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    payment_date TIMESTAMP,
    billing_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    notes TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES parking_user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (ticket_id) REFERENCES parking_tickets(ticket_id) ON DELETE CASCADE
);

CREATE INDEX idx_bills_invoice_number ON bills(invoice_number);
CREATE INDEX idx_bills_user_id ON bills(user_id);
CREATE INDEX idx_bills_ticket_id ON bills(ticket_id);
CREATE INDEX idx_bills_status ON bills(status);
CREATE INDEX idx_bills_billing_date ON bills(billing_date);
CREATE INDEX idx_bills_user_status ON bills(user_id, status);

-- =====================================================
-- 6. Sample Data Insertion (Optional)
-- =====================================================

-- Sample Parking Spaces
INSERT INTO parking_spaces (space_number, location, floor_number, slot_type, availability_status, rate_per_hour, is_active)
VALUES
    ('A1-001', 'A1', 1, 'NORMAL', 'available', 50.00, true),
    ('A1-002', 'A1', 1, 'NORMAL', 'available', 50.00, true),
    ('A1-003', 'A1', 1, 'VIP', 'available', 100.00, true),
    ('A1-004', 'A1', 1, 'HANDICAPPED', 'available', 0.00, true),
    ('A2-001', 'A2', 1, 'NORMAL', 'available', 50.00, true),
    ('A2-002', 'A2', 1, 'NORMAL', 'available', 50.00, true),
    ('A2-003', 'A2', 1, 'VIP', 'available', 100.00, true),
    ('B1-001', 'B1', 2, 'NORMAL', 'available', 50.00, true),
    ('B1-002', 'B1', 2, 'NORMAL', 'available', 50.00, true),
    ('B1-003', 'B1', 2, 'VIP', 'available', 100.00, true)
ON CONFLICT (space_number) DO NOTHING;

-- =====================================================
-- 7. View for Parking Dashboard
-- =====================================================

CREATE VIEW parking_dashboard AS
SELECT
    (SELECT COUNT(*) FROM parking_spaces WHERE availability_status = 'available' AND is_active = true) AS total_available,
    (SELECT COUNT(*) FROM parking_spaces WHERE slot_type = 'NORMAL' AND availability_status = 'available' AND is_active = true) AS normal_available,
    (SELECT COUNT(*) FROM parking_spaces WHERE slot_type = 'VIP' AND availability_status = 'available' AND is_active = true) AS vip_available,
    (SELECT COUNT(*) FROM parking_spaces WHERE slot_type = 'HANDICAPPED' AND availability_status = 'available' AND is_active = true) AS handicapped_available,
    (SELECT COUNT(*) FROM parking_tickets WHERE status = 'ACTIVE' AND exit_time IS NULL) AS current_occupancy,
    (SELECT COUNT(*) FROM parking_spaces WHERE is_active = true) AS total_spaces;

-- =====================================================
-- 8. View for Revenue Report
-- =====================================================

CREATE VIEW revenue_report AS
SELECT
    DATE(billing_date) AS billing_date,
    COUNT(*) AS total_bills,
    SUM(CASE WHEN status = 'PAID' THEN total_amount ELSE 0 END) AS daily_revenue,
    SUM(CASE WHEN status = 'PENDING' THEN total_amount ELSE 0 END) AS pending_amount,
    AVG(CASE WHEN status = 'PAID' THEN total_amount ELSE NULL END) AS avg_bill_amount
FROM bills
GROUP BY DATE(billing_date)
ORDER BY billing_date DESC;

-- =====================================================
-- 9. View for User Parking Statistics
-- =====================================================

CREATE VIEW user_parking_stats AS
SELECT
    u.user_id,
    u.user_name,
    u.user_email,
    COUNT(DISTINCT pt.ticket_id) AS total_parkings,
    COUNT(DISTINCT CASE WHEN pt.status = 'ACTIVE' THEN pt.ticket_id END) AS active_parkings,
    SUM(pt.duration_minutes) AS total_parking_minutes,
    SUM(CASE WHEN b.status = 'PAID' THEN b.total_amount ELSE 0 END) AS total_paid,
    SUM(CASE WHEN b.status = 'PENDING' THEN b.total_amount ELSE 0 END) AS total_pending
FROM parking_user u
LEFT JOIN parking_tickets pt ON u.user_id = pt.user_id
LEFT JOIN bills b ON pt.ticket_id = b.ticket_id
GROUP BY u.user_id, u.user_name, u.user_email;

-- =====================================================
-- 10. Stored Procedures (Optional)
-- =====================================================

-- Procedure to calculate occupancy percentage
CREATE OR REPLACE FUNCTION get_occupancy_percentage()
RETURNS DECIMAL(5, 2) AS $$
DECLARE
    total_spaces INTEGER;
    occupied_spaces INTEGER;
    occupancy_pct DECIMAL(5, 2);
BEGIN
    SELECT COUNT(*) INTO total_spaces FROM parking_spaces WHERE is_active = true;
    SELECT COUNT(*) INTO occupied_spaces FROM parking_tickets WHERE status = 'ACTIVE' AND exit_time IS NULL;

    IF total_spaces = 0 THEN
        occupancy_pct := 0;
    ELSE
        occupancy_pct := (occupied_spaces::DECIMAL / total_spaces) * 100;
    END IF;

    RETURN occupancy_pct;
END;
$$ LANGUAGE plpgsql;

-- Procedure to cleanup expired active tickets (for edge cases)
CREATE OR REPLACE FUNCTION cleanup_expired_tickets(hours_threshold INTEGER DEFAULT 24)
RETURNS INTEGER AS $$
DECLARE
    affected_rows INTEGER;
BEGIN
    UPDATE parking_tickets
    SET status = 'CLOSED'
    WHERE status = 'ACTIVE'
    AND exit_time IS NULL
    AND entry_time < NOW() - INTERVAL '1 hour' * hours_threshold;

    GET DIAGNOSTICS affected_rows = ROW_COUNT;
    RETURN affected_rows;
END;
$$ LANGUAGE plpgsql;

-- =====================================================
-- 11. Summary Statistics Query
-- =====================================================

-- Query to get all system statistics
-- SELECT * FROM parking_dashboard;
-- SELECT * FROM revenue_report;
-- SELECT * FROM user_parking_stats;
-- SELECT get_occupancy_percentage() AS occupancy_percentage;

-- =====================================================
-- End of Migration Script
-- =====================================================

