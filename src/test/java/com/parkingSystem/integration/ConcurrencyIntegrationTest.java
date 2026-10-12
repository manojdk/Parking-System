package com.parkingSystem.integration;

import com.parkingSystem.enums.PaymentMethod;
import com.parkingSystem.enums.ReservationStatus;
import com.parkingSystem.enums.SlotType;
import com.parkingSystem.enums.SpaceStatus;
import com.parkingSystem.enums.TicketStatus;
import com.parkingSystem.enums.VehicleType;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.model.ParkingTicket;
import com.parkingSystem.model.User;
import com.parkingSystem.model.Vehicle;
import com.parkingSystem.repository.BillRepository;
import com.parkingSystem.repository.ParkingSpaceRepository;
import com.parkingSystem.repository.ParkingTicketRepository;
import com.parkingSystem.repository.ReservationRepository;
import com.parkingSystem.repository.UserRepository;
import com.parkingSystem.repository.VehicleRepository;
import com.parkingSystem.service.IParkingEntryExitService;
import com.parkingSystem.service.IPaymentService;
import com.parkingSystem.service.IReservationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Many requests racing for the same space (or bill) at the same moment. Exactly one may win; everyone else must get a
 * clean 409 (ConflictException), never a second ticket or reservation and never an unexpected error.
 */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyIntegrationTest {

    private static final int DRIVERS = 8;
    private static final AtomicInteger COUNTER = new AtomicInteger();

    @Autowired
    private IParkingEntryExitService entryExitService;
    @Autowired
    private IReservationService reservationService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VehicleRepository vehicleRepository;
    @Autowired
    private ParkingSpaceRepository parkingSpaceRepository;
    @Autowired
    private ParkingTicketRepository parkingTicketRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private BillRepository billRepository;
    @Autowired
    private IPaymentService paymentService;

    @Test
    void manyCarsEnteringOneSpaceAtOnceGiveExactlyOneTicket() throws Exception {
        ParkingSpace space = newSpace();
        List<Callable<Object>> attempts = new ArrayList<>();
        for (int i = 0; i < DRIVERS; i++) {
            User driver = newUser();
            Vehicle car = newVehicle(driver);
            attempts.add(() -> entryExitService.recordVehicleEntry(driver.getUserId(), car.getVehicleId(),
                    space.getParkingSpaceId()));
        }

        Outcome outcome = runAtTheSameTime(attempts);

        assertThat(outcome.successes).isEqualTo(1);
        assertThat(outcome.conflicts).isEqualTo(DRIVERS - 1);
        assertThat(outcome.unexpected).isEmpty();
        long activeTickets = parkingTicketRepository.findByStatus(TicketStatus.ACTIVE).stream()
                .filter(t -> t.getParkingSpace().getParkingSpaceId().equals(space.getParkingSpaceId()))
                .count();
        assertThat(activeTickets).isEqualTo(1);
        assertThat(parkingSpaceRepository.findById(space.getParkingSpaceId()).orElseThrow().getAvailabilityStatus())
                .isEqualTo(SpaceStatus.OCCUPIED);
    }

    @Test
    void manyDriversReservingOneSpaceAtOnceGiveExactlyOneReservation() throws Exception {
        ParkingSpace space = newSpace();
        List<Callable<Object>> attempts = new ArrayList<>();
        for (int i = 0; i < DRIVERS; i++) {
            User driver = newUser();
            newVehicle(driver);
            attempts.add(() -> reservationService.createReservation(driver.getUserId(), space.getParkingSpaceId(), 1));
        }

        Outcome outcome = runAtTheSameTime(attempts);

        assertThat(outcome.successes).isEqualTo(1);
        assertThat(outcome.conflicts).isEqualTo(DRIVERS - 1);
        assertThat(outcome.unexpected).isEmpty();
        long active = reservationRepository.findAll().stream()
                .filter(r -> r.getParkingSpace().getParkingSpaceId().equals(space.getParkingSpaceId()))
                .filter(r -> r.getStatus() == ReservationStatus.ACTIVE)
                .count();
        assertThat(active).isEqualTo(1);
    }

    @Test
    void reservingAndDrivingInAtTheSameMomentNeverGivesBoth() throws Exception {
        ParkingSpace space = newSpace();
        User reserver = newUser();
        newVehicle(reserver);
        User walkIn = newUser();
        Vehicle walkInCar = newVehicle(walkIn);

        Outcome outcome = runAtTheSameTime(List.of(
                () -> reservationService.createReservation(reserver.getUserId(), space.getParkingSpaceId(), 1),
                () -> entryExitService.recordVehicleEntry(walkIn.getUserId(), walkInCar.getVehicleId(),
                        space.getParkingSpaceId())));

        assertThat(outcome.successes).isEqualTo(1);
        assertThat(outcome.conflicts).isEqualTo(1);
        assertThat(outcome.unexpected).isEmpty();
    }

    @Test
    void manyPaymentAttemptsForOneBillAtOnceStartExactlyOnePayment() throws Exception {
        User driver = newUser();
        Vehicle car = newVehicle(driver);
        ParkingSpace space = newSpace();
        ParkingTicket ticket = entryExitService.recordVehicleEntry(driver.getUserId(), car.getVehicleId(),
                space.getParkingSpaceId());
        entryExitService.recordVehicleExit(ticket.getTicketReference());
        Long billId = billRepository.findByParkingTicketTicketId(ticket.getTicketId()).orElseThrow().getBillId();

        List<Callable<Object>> attempts = new ArrayList<>();
        for (int i = 0; i < DRIVERS; i++) {
            String key = "parallel-key-" + i + "-" + System.nanoTime();
            attempts.add(() -> paymentService.startPayment(driver.getUserId(), billId, PaymentMethod.CARD, key));
        }

        Outcome outcome = runAtTheSameTime(attempts);

        assertThat(outcome.successes).isEqualTo(1);
        assertThat(outcome.conflicts).isEqualTo(DRIVERS - 1);
        assertThat(outcome.unexpected).isEmpty();
        assertThat(paymentService.getPaymentsForBill(billId)).hasSize(1);
    }

    @Test
    void databaseRejectsASecondActiveTicketForASpaceEvenWithoutTheLock() {
        ParkingSpace space = newSpace();
        User first = newUser();
        User second = newUser();
        parkingTicketRepository.save(activeTicket(first, newVehicle(first), space));

        // Bypasses the service (and its row lock) on purpose: the unique guard column is the safety net
        ParkingTicket duplicate = activeTicket(second, newVehicle(second), space);
        assertThatThrownBy(() -> parkingTicketRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsTheSameVehicleParkedTwice() {
        User driver = newUser();
        Vehicle car = newVehicle(driver);
        parkingTicketRepository.save(activeTicket(driver, car, newSpace()));

        ParkingTicket duplicate = activeTicket(driver, car, newSpace());
        assertThatThrownBy(() -> parkingTicketRepository.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // ---------------------------------------------------------------- helpers

    private record Outcome(int successes, int conflicts, List<Throwable> unexpected) {
    }

    /** Starts all attempts together (behind a latch) and sorts the results. */
    private static Outcome runAtTheSameTime(List<Callable<Object>> attempts) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(attempts.size());
        CountDownLatch ready = new CountDownLatch(attempts.size());
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> attempt : attempts) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return attempt.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            go.countDown();

            int successes = 0;
            int conflicts = 0;
            List<Throwable> unexpected = new ArrayList<>();
            for (Future<Object> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    successes++;
                } catch (java.util.concurrent.ExecutionException e) {
                    if (e.getCause() instanceof ConflictException) {
                        conflicts++;
                    } else {
                        unexpected.add(e.getCause());
                    }
                }
            }
            return new Outcome(successes, conflicts, unexpected);
        } finally {
            pool.shutdownNow();
        }
    }

    private ParkingSpace newSpace() {
        return parkingSpaceRepository.save(ParkingSpace.builder()
                .spaceNumber("C-" + COUNTER.incrementAndGet() + "-" + System.nanoTime())
                .location("Concurrency")
                .floorNumber(0)
                .slotType(SlotType.NORMAL)
                .ratePerHour(BigDecimal.TEN)
                .availabilityStatus(SpaceStatus.AVAILABLE)
                .isActive(true)
                .build());
    }

    private User newUser() {
        String name = "race" + COUNTER.incrementAndGet() + "_" + System.nanoTime();
        User user = new User();
        user.setUserName(name);
        user.setUserEmail(name + "@example.com");
        user.setPassword("not-used");
        user.setLicencePlate("LP-" + name);
        user.setRole("USER");
        user.setCreatedDate(LocalDateTime.now());
        return userRepository.save(user);
    }

    private Vehicle newVehicle(User owner) {
        return vehicleRepository.save(Vehicle.builder()
                .user(owner)
                .licensePlate("CAR-" + COUNTER.incrementAndGet() + "-" + System.nanoTime())
                .vehicleType(VehicleType.CAR)
                .isActive(true)
                .build());
    }

    private static ParkingTicket activeTicket(User user, Vehicle vehicle, ParkingSpace space) {
        return ParkingTicket.builder()
                .user(user)
                .vehicle(vehicle)
                .parkingSpace(space)
                .entryTime(LocalDateTime.now())
                .status(TicketStatus.ACTIVE)
                .build();
    }
}
