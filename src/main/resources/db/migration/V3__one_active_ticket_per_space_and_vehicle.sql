-- Step 3: the database itself guarantees at most one ACTIVE ticket per parking space and per vehicle.
--
-- PostgreSQL could do this with a partial unique index (... where status = 'ACTIVE'), but the tests run on
-- H2, which has no partial indexes. The portable pattern used here: the application copies the space id
-- and vehicle id into these columns while the ticket is ACTIVE and clears them otherwise; a plain unique
-- constraint ignores NULLs, so only ACTIVE tickets compete. The row lock taken during entry is the first
-- line of defence; these constraints are the safety net.
--
-- If this migration fails on an existing database, it already contains two ACTIVE tickets for the same
-- space or vehicle (the double-booking bug). Close the duplicates by hand and run it again.

alter table parking_tickets add column active_space_id bigint;
alter table parking_tickets add column active_vehicle_id bigint;

update parking_tickets set active_space_id = parking_space_id, active_vehicle_id = vehicle_id
 where status = 'ACTIVE';

alter table parking_tickets add constraint uk_parking_tickets_active_space unique (active_space_id);
alter table parking_tickets add constraint uk_parking_tickets_active_vehicle unique (active_vehicle_id);
