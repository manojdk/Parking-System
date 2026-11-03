-- Step 7: two new slot types, BIKE (two-wheeler bay) and EV_CHARGING (bay with a charger).
-- Which vehicle may use which slot type is a business rule in SlotType.accepts(); the database only
-- restricts the vocabulary.
--
-- The old check constraint has our name on databases created by V1, and Hibernate's generated name on
-- databases that were baselined from the pre-Flyway schema, so both are dropped if present.

alter table parking_spaces drop constraint if exists ck_parking_spaces_slot_type;
alter table parking_spaces drop constraint if exists parking_spaces_slot_type_check;

alter table parking_spaces add constraint ck_parking_spaces_slot_type
    check (slot_type in ('NORMAL', 'VIP', 'HANDICAPPED', 'EV_CHARGING', 'BIKE'));
