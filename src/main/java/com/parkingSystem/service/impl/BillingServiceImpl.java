package com.parkingSystem.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.*;
import com.parkingSystem.pricing.PricingCalculator;
import com.parkingSystem.pricing.PricingCalculator.Quote;
import com.parkingSystem.pricing.PricingMode;
import com.parkingSystem.repository.BillRepository;
import com.parkingSystem.repository.ParkingTicketRepository;
import com.parkingSystem.service.IPaymentService;
import com.parkingSystem.enums.BillingStatus;
import com.parkingSystem.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for billing and invoice management. The price itself comes from {@link PricingCalculator}.
 */
@Service
@AllArgsConstructor
@Slf4j
public class BillingServiceImpl implements com.parkingSystem.service.IBillingService {

    /** Payment method recorded on a bill that cost nothing (grace period). */
    static final String NO_CHARGE = "NO_CHARGE";

    private BillRepository billRepository;
    private ParkingTicketRepository parkingTicketRepository;
    private Clock clock;
    private IPaymentService paymentService;
    private PricingCalculator pricingCalculator;

    /**
     * Bill a closed ticket with the configured pricing mode ({@code app.pricing.mode}). Used on vehicle exit.
     */
    @Transactional
    public Bill generateBill(Long ticketId) {
        return generateBill(ticketId, pricingCalculator.defaultMode());
    }

    /**
     * Bill a closed ticket with the given pricing mode.
     */
    @Transactional
    public Bill generateBill(Long ticketId, PricingMode mode) {
        log.info("Generating {} bill for ticket ID: {}", mode, ticketId);
        ParkingTicket ticket = getBillableTicket(ticketId);
        Quote quote = pricingCalculator.quote(ticket.getDurationMinutes(), ticket.getParkingSpace().getRatePerHour(),
                mode);

        Bill savedBill = saveBill(ticket, quote);
        log.info("Bill generated successfully. Invoice number: {}, Amount: {}",
                savedBill.getInvoiceNumber(), savedBill.getTotalAmount());
        return savedBill;
    }

    /**
     * Loads a ticket and checks it can be billed: it must be closed and not billed yet.
     */
    private ParkingTicket getBillableTicket(Long ticketId) {
        ParkingTicket ticket = parkingTicketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found with ID: " + ticketId));

        if (ticket.getStatus() != TicketStatus.CLOSED || ticket.getDurationMinutes() == null) {
            throw new BadRequestException("Only closed tickets can be billed. Vehicle hasn't exited yet.");
        }
        if (billRepository.existsByParkingTicketTicketId(ticketId)) {
            throw new ConflictException("Bill already exists for this ticket");
        }
        return ticket;
    }

    /**
     * Stores the bill. A free stay (grace period) still gets a bill, so every closed ticket has exactly one,
     * but it is settled immediately with nothing to pay.
     */
    private Bill saveBill(ParkingTicket ticket, Quote quote) {
        LocalDateTime now = LocalDateTime.now(clock);
        Bill bill = Bill.builder()
                .invoiceNumber(newInvoiceNumber())
                .user(ticket.getUser())
                .parkingTicket(ticket)
                .ratePerHour(ticket.getParkingSpace().getRatePerHour())
                .durationHours(quote.billableHours())
                .baseAmount(quote.baseAmount())
                .taxAmount(quote.taxAmount())
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(quote.totalAmount())
                .status(quote.freeStay() ? BillingStatus.PAID : BillingStatus.PENDING)
                .paymentMethod(quote.freeStay() ? NO_CHARGE : null)
                .paymentDate(quote.freeStay() ? now : null)
                .billingDate(now)
                .notes(quote.notes())
                .build();
        return billRepository.save(bill);
    }

    /**
     * Invoice numbers are UUID based so they are unique without a database round trip.
     */
    static String newInvoiceNumber() {
        return "INV-" + UUID.randomUUID().toString().toUpperCase();
    }

    /**
     * Record an offline payment (admin). The bill row is locked so this cannot race an online payment's
     * webhook; it is refused while an online payment is still in progress.
     */
    @Transactional
    public Bill markBillAsPaid(Long billId, String paymentMethod) {
        log.info("Marking bill ID: {} as paid", billId);
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new BadRequestException("paymentMethod is mandatory");
        }

        Bill bill = lockBill(billId);
        if (bill.getStatus() != BillingStatus.PENDING) {
            throw new ConflictException("Bill cannot be paid. Current status: " + bill.getStatus());
        }
        if (paymentService.hasPaymentInProgress(billId)) {
            throw new ConflictException("An online payment for this bill is in progress");
        }

        bill.setStatus(BillingStatus.PAID);
        bill.setPaymentMethod(paymentMethod);
        bill.setPaymentDate(LocalDateTime.now(clock));

        Bill updatedBill = billRepository.save(bill);
        log.info("Bill marked as paid successfully");
        return updatedBill;
    }

    private Bill lockBill(Long billId) {
        return billRepository.findByIdForUpdate(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + billId));
    }

    /**
     * Get bill by ID
     */
    public Bill getBillById(Long billId) {
        return billRepository.findById(billId)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with ID: " + billId));
    }

    /**
     * Get the bill generated for a ticket, if any
     */
    public Optional<Bill> getBillForTicket(Long ticketId) {
        return billRepository.findByParkingTicketTicketId(ticketId);
    }

    /**
     * Get bill by invoice number
     */
    public Optional<Bill> getBillByInvoiceNumber(String invoiceNumber) {
        log.info("Fetching bill by invoice number: {}", invoiceNumber);
        return billRepository.findByInvoiceNumber(invoiceNumber);
    }

    /**
     * Get user's bills
     */
    public Page<Bill> getUserBills(Long userId, Pageable pageable) {
        log.info("Fetching bills for user ID: {}", userId);
        return billRepository.findByUserUserId(userId, pageable);
    }

    /**
     * Get pending bills for user
     */
    public Page<Bill> getUserPendingBills(Long userId, Pageable pageable) {
        log.info("Fetching pending bills for user ID: {}", userId);
        return billRepository.findByUserUserIdAndStatus(userId, BillingStatus.PENDING, pageable);
    }

    /**
     * Apply discount to bill
     */
    @Transactional
    public Bill applyDiscount(Long billId, BigDecimal discountAmount) {
        log.info("Applying discount to bill ID: {}", billId);
        if (discountAmount == null || discountAmount.signum() < 0) {
            throw new BadRequestException("Discount amount must be zero or positive");
        }

        Bill bill = lockBill(billId);
        if (bill.getStatus() != BillingStatus.PENDING) {
            throw new ConflictException("Discount can only be applied to pending bills");
        }
        // The provider is charging the current total; changing it now would make the payment not match the bill
        if (paymentService.hasPaymentInProgress(billId)) {
            throw new ConflictException("Cannot change the amount while an online payment is in progress");
        }

        BigDecimal grossAmount = bill.getBaseAmount().add(bill.getTaxAmount());
        if (discountAmount.compareTo(grossAmount) > 0) {
            throw new BadRequestException("Discount cannot exceed the bill amount");
        }

        bill.setDiscountAmount(discountAmount);
        bill.setTotalAmount(grossAmount.subtract(discountAmount));

        Bill updatedBill = billRepository.save(bill);
        log.info("Discount applied successfully");
        return updatedBill;
    }

    /**
     * Get all pending bills
     */
    public Page<Bill> getAllPendingBills(Pageable pageable) {
        log.info("Fetching all pending bills");
        return billRepository.findByStatus(BillingStatus.PENDING, pageable);
    }

    /**
     * Total of all PAID bills, summed by the database instead of loading every bill into memory.
     */
    public BigDecimal getTotalRevenue() {
        log.info("Calculating total revenue");
        return billRepository.sumTotalAmountByStatus(BillingStatus.PAID);
    }
}
