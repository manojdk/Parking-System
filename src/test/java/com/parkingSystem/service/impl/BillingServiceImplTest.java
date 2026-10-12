package com.parkingSystem.service.impl;

import com.parkingSystem.enums.BillingStatus;
import com.parkingSystem.enums.TicketStatus;
import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.Bill;
import com.parkingSystem.model.ParkingSpace;
import com.parkingSystem.model.ParkingTicket;
import com.parkingSystem.model.User;
import com.parkingSystem.repository.BillRepository;
import com.parkingSystem.repository.ParkingTicketRepository;
import com.parkingSystem.service.IPaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {

    private static final Long TICKET_ID = 42L;
    private static final BigDecimal RATE = new BigDecimal("10.00");

    @Mock
    private BillRepository billRepository;

    @Mock
    private ParkingTicketRepository parkingTicketRepository;

    @Mock
    private IPaymentService paymentService;

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-12T10:00:00Z"), ZoneOffset.UTC);

    private BillingServiceImpl billingService;

    @BeforeEach
    void setUp() {
        billingService = new BillingServiceImpl(billRepository, parkingTicketRepository, clock, paymentService);
        lenient().when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void hourlyBillRoundsStartedHoursUpAndAddsTax() {
        givenClosedTicket(150); // 2.5 h -> 3 billable hours

        Bill bill = billingService.generateBill(TICKET_ID);

        assertThat(bill.getDurationHours()).isEqualByComparingTo("3");
        assertThat(bill.getBaseAmount()).isEqualByComparingTo("30.00");
        assertThat(bill.getTaxAmount()).isEqualByComparingTo("3.00");
        assertThat(bill.getDiscountAmount()).isEqualByComparingTo("0");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("33.00");
        assertThat(bill.getStatus()).isEqualTo(BillingStatus.PENDING);
        assertThat(bill.getBillingDate()).as("billing time comes from the injected clock")
                .isEqualTo(LocalDateTime.of(2026, 10, 12, 10, 0));
        assertThat(bill.getInvoiceNumber()).startsWith("INV-");
    }

    @Test
    void stayUnderOneMinuteIsChargedTheMinimumUnit() {
        givenClosedTicket(0);

        Bill bill = billingService.generateBill(TICKET_ID);

        assertThat(bill.getDurationHours()).isEqualByComparingTo("1");
        assertThat(bill.getBaseAmount()).isEqualByComparingTo("10.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("11.00");
    }

    @ParameterizedTest
    @CsvSource({ "0,1", "1,1", "59,1", "60,1", "61,2", "180,3", "-5,1" })
    void billableHoursHasAMinimumOfOneAndRoundsUp(long minutes, long expectedHours) {
        assertThat(BillingServiceImpl.billableHours(minutes)).isEqualByComparingTo(String.valueOf(expectedHours));
    }

    @ParameterizedTest
    @CsvSource({
            "1, 10.00",   // first hour full rate
            "3, 28.00",   // 10 + 2 * 9
            "5, 44.00",   // 10 + 18 + 2 * 8
            "10, 82.00"   // 10 + 18 + 40 + 2 * 7
    })
    void slabPricingAppliesDecreasingRates(long hours, String expected) {
        BigDecimal amount = BillingServiceImpl.calculateSlabBasedAmount(BigDecimal.valueOf(hours), RATE);
        assertThat(amount).isEqualByComparingTo(expected);
    }

    @Test
    void slabBillUsesSlabAmount() {
        givenClosedTicket(5 * 60);

        Bill bill = billingService.generateBillWithSlabPricing(TICKET_ID);

        assertThat(bill.getBaseAmount()).isEqualByComparingTo("44.00");
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("48.40");
        assertThat(bill.getNotes()).contains("Slab");
    }

    @Test
    void duplicateHourlyBillIsRejected() {
        givenClosedTicket(30);
        when(billRepository.existsByParkingTicketTicketId(TICKET_ID)).thenReturn(true);

        assertThatThrownBy(() -> billingService.generateBill(TICKET_ID)).isInstanceOf(ConflictException.class);
        verify(billRepository, never()).save(any());
    }

    @Test
    void duplicateSlabBillIsRejected() {
        givenClosedTicket(30);
        when(billRepository.existsByParkingTicketTicketId(TICKET_ID)).thenReturn(true);

        assertThatThrownBy(() -> billingService.generateBillWithSlabPricing(TICKET_ID))
                .isInstanceOf(ConflictException.class);
        verify(billRepository, never()).save(any());
    }

    @Test
    void activeTicketCannotBeBilled() {
        ParkingTicket ticket = ticket(null);
        ticket.setStatus(TicketStatus.ACTIVE);
        when(parkingTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> billingService.generateBill(TICKET_ID)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void unknownTicketIsNotFound() {
        when(parkingTicketRepository.findById(TICKET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> billingService.generateBill(TICKET_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void invoiceNumbersAreUnique() {
        assertThat(BillingServiceImpl.newInvoiceNumber()).isNotEqualTo(BillingServiceImpl.newInvoiceNumber());
    }

    @Test
    void paidBillCannotBePaidAgain() {
        Bill bill = Bill.builder().billId(1L).status(BillingStatus.PAID).build();
        when(billRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(bill));

        assertThatThrownBy(() -> billingService.markBillAsPaid(1L, "CARD")).isInstanceOf(ConflictException.class);
    }

    @Test
    void discountCannotExceedBillAmount() {
        Bill bill = Bill.builder().billId(1L).status(BillingStatus.PENDING)
                .baseAmount(new BigDecimal("10.00")).taxAmount(new BigDecimal("1.00")).build();
        when(billRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(bill));

        assertThatThrownBy(() -> billingService.applyDiscount(1L, new BigDecimal("12.00")))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void discountReducesTotal() {
        Bill bill = Bill.builder().billId(1L).status(BillingStatus.PENDING)
                .baseAmount(new BigDecimal("10.00")).taxAmount(new BigDecimal("1.00")).build();
        when(billRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(bill));

        Bill updated = billingService.applyDiscount(1L, new BigDecimal("2.00"));

        assertThat(updated.getTotalAmount()).isEqualByComparingTo("9.00");
    }

    @Test
    void cashPaymentMarksThePendingBillPaid() {
        Bill bill = Bill.builder().billId(1L).status(BillingStatus.PENDING).build();
        when(billRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(bill));

        Bill paid = billingService.markBillAsPaid(1L, "CASH");

        assertThat(paid.getStatus()).isEqualTo(BillingStatus.PAID);
        assertThat(paid.getPaymentMethod()).isEqualTo("CASH");
        assertThat(paid.getPaymentDate()).isEqualTo(LocalDateTime.of(2026, 10, 12, 10, 0));
    }

    @Test
    void cashPaymentIsRefusedWhileAnOnlinePaymentIsInProgress() {
        Bill bill = Bill.builder().billId(1L).status(BillingStatus.PENDING).build();
        when(billRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(bill));
        when(paymentService.hasPaymentInProgress(1L)).thenReturn(true);

        assertThatThrownBy(() -> billingService.markBillAsPaid(1L, "CASH")).isInstanceOf(ConflictException.class);
        assertThat(bill.getStatus()).isEqualTo(BillingStatus.PENDING);
    }

    @Test
    void discountIsRefusedWhileAnOnlinePaymentIsInProgress() {
        Bill bill = Bill.builder().billId(1L).status(BillingStatus.PENDING)
                .baseAmount(new BigDecimal("10.00")).taxAmount(new BigDecimal("1.00"))
                .totalAmount(new BigDecimal("11.00")).build();
        when(billRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(bill));
        when(paymentService.hasPaymentInProgress(1L)).thenReturn(true);

        assertThatThrownBy(() -> billingService.applyDiscount(1L, new BigDecimal("2.00")))
                .isInstanceOf(ConflictException.class);
        assertThat(bill.getTotalAmount()).isEqualByComparingTo("11.00");
    }

    private void givenClosedTicket(long durationMinutes) {
        when(parkingTicketRepository.findById(TICKET_ID)).thenReturn(Optional.of(ticket(durationMinutes)));
    }

    private static ParkingTicket ticket(Long durationMinutes) {
        User user = new User();
        user.setUserId(7L);
        ParkingSpace space = ParkingSpace.builder().parkingSpaceId(3L).ratePerHour(RATE).build();
        return ParkingTicket.builder()
                .ticketId(TICKET_ID)
                .user(user)
                .parkingSpace(space)
                .durationMinutes(durationMinutes)
                .status(TicketStatus.CLOSED)
                .build();
    }
}
