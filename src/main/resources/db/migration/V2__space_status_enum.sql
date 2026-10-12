-- Step 2: parking-space status becomes a fixed vocabulary (enum SpaceStatus).
--
-- Before this, 'occupied' meant either "a car is parked here" or "someone reserved it", so a reserved
-- space rejected its own owner at the gate. From now on a space is OCCUPIED only while it has an ACTIVE
-- ticket. Spaces that were 'occupied' only because of a legacy reservation are released here; the legacy
-- reservations themselves are closed as EXPIRED in V4 (they never had an end time).

update parking_spaces set availability_status = 'OCCUPIED'
 where exists (select 1 from parking_tickets t
                where t.parking_space_id = parking_spaces.parking_space_id and t.status = 'ACTIVE');

update parking_spaces set availability_status = 'AVAILABLE'
 where availability_status <> 'OCCUPIED';

alter table parking_spaces alter column availability_status set data type varchar(20);

alter table parking_spaces add constraint ck_parking_spaces_status
    check (availability_status in ('AVAILABLE', 'RESERVED', 'OCCUPIED', 'OUT_OF_SERVICE'));
