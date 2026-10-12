package com.parkingSystem.service.impl;

import com.parkingSystem.enums.ReservationStatus;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.enums.TicketStatus;
import com.parkingSystem.enums.VehicleType;
import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.model.ParkingTicket;
import com.parkingSystem.model.Reservation;
import com.parkingSystem.model.User;
import com.parkingSystem.model.Vehicle;
import com.parkingSystem.repository.ParkingSpaceRepository;
import com.parkingSystem.repository.ParkingTicketRepository;
import com.parkingSystem.repository.ReservationRepository;
import com.parkingSystem.service.IBillingService;
import com.parkingSystem.service.IUserService;
import com.parkingSystem.service.IVehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ParkingEntryExitServiceImplTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 12, 10, 0);

    @Mock
    private ParkingTicketRepository parkingTicketRepository;
    @Mock
    private ParkingSpaceRepository parkingSpaceRepository;
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private IVehicleService vehicleService;
    @Mock
    private IUserService userService;
    @Mock
    private IBillingService billingService;

    private ParkingEntryExitServiceImpl service;

    private final User owner = user(1L);
    private final Vehicle vehicle = vehicle(10L, owner);
    private final ParkingSpace space = space(100L, SpaceStatus.AVAILABLE);

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new ParkingEntryExitServiceImpl(parkingTicketRepository, parkingSpaceRepository,
                reservationRepository, vehicleService, userService, billingService, clock);
    }

    // ---------------------------------------------------------------- entry: walk-in

    @Test
    void entryLocksTheSpaceCreatesActiveTicketAndOccupiesSpace() {
        givenEntryLookups();
        givenTicketSaveReturnsIt();

        ParkingTicket ticket = service.recordVehicleEntry(1L, 10L, 100L);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(ticket.getEntryTime()).isEqualTo(NOW);
        assertThat(space.getAvailabilityStatus()).isEqualTo(SpaceStatus.OCCUPIED);
        verify(parkingSpaceRepository).findByIdForUpdate(100L);
        verify(parkingSpaceRepository).save(space);
    }

    @Test
    void entryIntoOccupiedSpaceIsAConflict() {
        space.setAvailabilityStatus(SpaceStatus.OCCUPIED);
        givenEntryLookups();

        assertThatThrownBy(() -> service.recordVehicleEntry(1L, 10L, 100L)).isInstanceOf(ConflictException.class);
        verify(parkingTicketRepository, never()).save(any());
    }

    @Test
    void entryIntoOutOfServiceSpaceIsAConflict() {
        space.setAvailabilityStatus(SpaceStatus.OUT_OF_SERVICE);
        givenEntryLookups();

        assertThatThrownBy(() -> service.recordVehicleEntry(1L, 10L, 100L))
                .isInstanceOf(ConflictException.class).hasMessageContaining("out of service");
    }

    @Test
    void entryIntoRemovedSpaceIsRejected() {
        space.setIsActive(false);
        givenEntryLookups();

        assertThatThrownBy(() -> service.recordVehicleEntry(1L, 10L, 100L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void entryWithSomeoneElsesVehicleIsRejected() {
        when(parkingSpaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(space));
        when(userService.getUserById(2L)).thenReturn(Optional.of(user(2L)));
        when(vehicleService.getVehicleById(10L)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> service.recordVehicleEntry(2L, 10L, 100L))
                .isInstanceOf(BadRequestException.class);
        verify(parkingTicketRepository, never()).save(any());
    }

    @Test
    void deactivatedVehicleCannotEnter() {
        vehicle.setIsActive(false);
        givenEntryLookups();

        assertThatThrownBy(() -> service.recordVehicleEntry(1L, 10L, 100L))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("deactivated");
    }

    @Test
    void vehicleMustFitTheSlotType() {
        space.setSlotType(SlotType.BIKE);
        givenEntryLookups();

        assertThatThrownBy(() -> service.recordVehicleEntry(1L, 10L, 100L))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("CAR cannot park in a BIKE space");
        verify(parkingTicketRepository, never()).save(any());
    }

    @Test
    void onlyEvsMayUseAChargingBay() {
        space.setSlotType(SlotType.EV_CHARGING);
        givenEntryLookups();

        assertThatThrownBy(() -> service.recordVehicleEntry(1L, 10L, 100L)).isInstanceOf(BadRequestException.class);

        vehicle.setVehicleType(VehicleType.EV);
        givenTicketSaveReturnsIt();
        assertThat(service.recordVehicleEntry(1L, 10L, 100L).getStatus()).isEqualTo(TicketStatus.ACTIVE);
    }

    @Test
    void vehicleThatIsAlreadyParkedCannotEnterAgain() {
        givenEntryLookups();
        when(parkingTicketRepository.existsByVehicleVehicleIdAndStatus(10L, TicketStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.recordVehicleEntry(1L, 10L, 100L)).isInstanceOf(ConflictException.class);
        verify(parkingTicketRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- entry: reserved space

    @Test
    void ownerOfTheReservationCanEnterAndTheReservationIsFulfilled() {
        space.setAvailabilityStatus(SpaceStatus.RESERVED);
        Reservation reservation = activeReservation(owner, NOW.plusMinutes(10));
        givenEntryLookups();
        when(reservationRepository.findActiveBySpaceIdForUpdate(100L)).thenReturn(Optional.of(reservation));
        givenTicketSaveReturnsIt();

        ParkingTicket ticket = service.recordVehicleEntry(1L, 10L, 100L);

        assertThat(space.getAvailabilityStatus()).isEqualTo(SpaceStatus.OCCUPIED);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.FULFILLED);
        assertThat(reservation.getFulfilledAt()).isEqualTo(NOW);
        assertThat(reservation.getTicket()).isSameAs(ticket);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void anotherDriverCannotTakeAReservedSpace() {
        space.setAvailabilityStatus(SpaceStatus.RESERVED);
        givenEntryLookups();
        when(reservationRepository.findActiveBySpaceIdForUpdate(100L))
                .thenReturn(Optional.of(activeReservation(user(2L), NOW.plusMinutes(10))));

        assertThatThrownBy(() -> service.recordVehicleEntry(1L, 10L, 100L))
                .isInstanceOf(ConflictException.class).hasMessageContaining("reserved for another driver");
        verify(parkingTicketRepository, never()).save(any());
        assertThat(space.getAvailabilityStatus()).isEqualTo(SpaceStatus.RESERVED);
    }

    @Test
    void reservationWhoseHoldRanOutIsExpiredAtTheGateAndTheSpaceIsFree() {
        space.setAvailabilityStatus(SpaceStatus.RESERVED);
        Reservation stale = activeReservation(user(2L), NOW.minusMinutes(1));
        givenEntryLookups();
        when(reservationRepository.findActiveBySpaceIdForUpdate(100L)).thenReturn(Optional.of(stale));
        givenTicketSaveReturnsIt();

        ParkingTicket ticket = service.recordVehicleEntry(1L, 10L, 100L);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.ACTIVE);
        assertThat(stale.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(space.getAvailabilityStatus()).isEqualTo(SpaceStatus.OCCUPIED);
    }

    @Test
    void reservedSpaceWithoutAnOpenReservationIsRepairedInsteadOfBlocked() {
        space.setAvailabilityStatus(SpaceStatus.RESERVED);
        givenEntryLookups();
        when(reservationRepository.findActiveBySpaceIdForUpdate(100L)).thenReturn(Optional.empty());
        givenTicketSaveReturnsIt();

        service.recordVehicleEntry(1L, 10L, 100L);

        assertThat(space.getAvailabilityStatus()).isEqualTo(SpaceStatus.OCCUPIED);
    }

    // ---------------------------------------------------------------- exit / cancel

    @Test
    void exitClosesTicketFreesSpaceAndBills() {
        space.setAvailabilityStatus(SpaceStatus.OCCUPIED);
        ParkingTicket ticket = activeTicket(NOW.minusMinutes(95));
        givenTicketLookups(ticket);
        when(parkingTicketRepository.save(ticket)).thenReturn(ticket);

        ParkingTicket closed = service.recordVehicleExit("TKT-1");

        assertThat(closed.getStatus()).isEqualTo(TicketStatus.CLOSED);
        assertThat(closed.getExitTime()).isEqualTo(NOW);
        assertThat(closed.getDurationMinutes()).isEqualTo(95L);
        assertThat(space.getAvailabilityStatus()).isEqualTo(SpaceStatus.AVAILABLE);
        verify(parkingSpaceRepository).findByIdForUpdate(100L);
        verify(billingService).generateBill(500L);
    }

    @Test
    void exitFailsClearlyWhenBillingFails() {
        space.setAvailabilityStatus(SpaceStatus.OCCUPIED);
        ParkingTicket ticket = activeTicket(NOW.minusMinutes(5));
        givenTicketLookups(ticket);
        when(parkingTicketRepository.save(ticket)).thenReturn(ticket);
        when(billingService.generateBill(500L)).thenThrow(new ConflictException("Bill already exists for this ticket"));

        assertThatThrownBy(() -> service.recordVehicleExit("TKT-1")).isInstanceOf(ConflictException.class);
    }

    @Test
    void exitOfClosedTicketIsAConflictAndDoesNotBill() {
        ParkingTicket ticket = activeTicket(NOW.minusMinutes(5));
        ticket.setStatus(TicketStatus.CLOSED);
        givenTicketLookups(ticket);

        assertThatThrownBy(() -> service.recordVehicleExit("TKT-1")).isInstanceOf(ConflictException.class);
        verify(billingService, never()).generateBill(anyLong());
    }

    @Test
    void cancelReleasesSpaceOfActiveTicket() {
        space.setAvailabilityStatus(SpaceStatus.OCCUPIED);
        ParkingTicket ticket = activeTicket(NOW.minusMinutes(5));
        givenTicketLookups(ticket);
        when(parkingTicketRepository.save(ticket)).thenReturn(ticket);

        ParkingTicket cancelled = service.cancelTicket("TKT-1", "changed plans");

        assertThat(cancelled.getStatus()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(space.getAvailabilityStatus()).isEqualTo(SpaceStatus.AVAILABLE);
        verify(billingService, never()).generateBill(anyLong());
    }

    @Test
    void closedTicketCannotBeCancelled() {
        ParkingTicket ticket = activeTicket(NOW.minusMinutes(5));
        ticket.setStatus(TicketStatus.CLOSED);
        givenTicketLookups(ticket);

        assertThatThrownBy(() -> service.cancelTicket("TKT-1", "too late")).isInstanceOf(ConflictException.class);
    }

    // ---------------------------------------------------------------- helpers

    private void givenEntryLookups() {
        when(parkingSpaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(space));
        when(userService.getUserById(1L)).thenReturn(Optional.of(owner));
        when(vehicleService.getVehicleById(10L)).thenReturn(Optional.of(vehicle));
    }

    private void givenTicketSaveReturnsIt() {
        when(parkingTicketRepository.save(any(ParkingTicket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void givenTicketLookups(ParkingTicket ticket) {
        when(parkingTicketRepository.findSpaceIdByTicketReference("TKT-1")).thenReturn(Optional.of(100L));
        when(parkingSpaceRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(space));
        when(parkingTicketRepository.findByTicketReferenceForUpdate("TKT-1")).thenReturn(Optional.of(ticket));
    }

    private ParkingTicket activeTicket(LocalDateTime entryTime) {
        return ParkingTicket.builder()
                .ticketId(500L)
                .ticketReference("TKT-1")
                .user(owner)
                .vehicle(vehicle)
                .parkingSpace(space)
                .entryTime(entryTime)
                .status(TicketStatus.ACTIVE)
                .build();
    }

    private Reservation activeReservation(User holder, LocalDateTime expiresAt) {
        Reservation reservation = new Reservation();
        reservation.setReservationId(700L);
        reservation.setUser(holder);
        reservation.setParkingSpace(space);
        reservation.setReservationTime(expiresAt.minusMinutes(30));
        reservation.setExpiresAt(expiresAt);
        reservation.setStatus(ReservationStatus.ACTIVE);
        return reservation;
    }

    private static User user(Long id) {
        User user = new User();
        user.setUserId(id);
        user.setUserName("user" + id);
        return user;
    }

    private static Vehicle vehicle(Long id, User owner) {
        return Vehicle.builder().vehicleId(id).user(owner).licensePlate("PLATE-" + id).isActive(true)
                .vehicleType(VehicleType.CAR).build();
    }

    private static ParkingSpace space(Long id, SpaceStatus status) {
        return ParkingSpace.builder()
                .parkingSpaceId(id)
                .spaceNumber("S-" + id)
                .slotType(SlotType.NORMAL)
                .ratePerHour(BigDecimal.TEN)
                .availabilityStatus(status)
                .isActive(true)
                .build();
    }
}
