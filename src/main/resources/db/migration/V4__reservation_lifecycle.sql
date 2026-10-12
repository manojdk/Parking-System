-- Step 4: reservations get a lifecycle.
--
-- A reservation now holds a space for a limited time (app.reservation.hold-minutes). It ends in exactly one
-- of: FULFILLED (the owner drove in; linked to the ticket), CANCELLED (by the owner or an admin) or
-- EXPIRED (no-show, closed by the scheduled expiry job). Rows are never deleted, so feedback can keep
-- pointing at them.

alter table parking_reservations add column status varchar(20);
alter table parking_reservations add column expires_at timestamp(6);
alter table parking_reservations add column fulfilled_at timestamp(6);
alter table parking_reservations add column cancelled_at timestamp(6);
alter table parking_reservations add column updated_at timestamp(6);
alter table parking_reservations add column ticket_id bigint;
alter table parking_reservations add column active_space_id bigint;
alter table parking_reservations add column active_user_id bigint;

-- Legacy reservations had no end time and blocked their space forever; close them all.
update parking_reservations
   set status = 'EXPIRED', expires_at = reservation_time, updated_at = reservation_time;

alter table parking_reservations alter column status set not null;
alter table parking_reservations alter column expires_at set not null;

alter table parking_reservations add constraint ck_reservations_status
    check (status in ('ACTIVE', 'FULFILLED', 'CANCELLED', 'EXPIRED'));
alter table parking_reservations add constraint fk_reservations_ticket
    foreign key (ticket_id) references parking_tickets (ticket_id);
-- Same NULL-ignoring pattern as V3: one ACTIVE reservation per space, and per user.
alter table parking_reservations add constraint uk_reservations_active_space unique (active_space_id);
alter table parking_reservations add constraint uk_reservations_active_user unique (active_user_id);

-- The expiry job looks for ACTIVE reservations whose hold has run out.
create index ix_reservations_status_expires on parking_reservations (status, expires_at);
