package com.parkingSystem.service.impl;

import com.parkingSystem.exception.BadRequestException;
import com.parkingSystem.exception.ConflictException;
import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.model.*;
import com.parkingSystem.repository.BillRepository;
import com.parkingSystem.repository.ParkingTicketRepository;
import com.parkingSystem.enums.BillingStatus;
import com.parkingSystem.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for billing and invoice management
 */
@Service
@AllArgsConstructor
@Slf4j
public class BillingServiceImpl implements com.parkingSystem.service.IBillingService {

    /** Flat tax rate applied to the base amount (10%). */
    static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private BillRepository billRepository;
    private ParkingTicketRepository parkingTicketRepository;
    private Clock clock;

    /**
     * Generate bill for a parking ticket (hourly billing)
     */
    @Transactional
    public Bill generateBill(Long ticketId) {
        log.info("Generating bill for ticket ID: {}", ticketId);
        ParkingTicket ticket = getBillableTicket(ticketId);

        BigDecimal durationHours = billableHours(ticket.getDurationMinutes());
        BigDecimal ratePerHour = ticket.getParkingSpace().getRatePerHour();
        BigDecimal baseAmount = ratePerHour.multiply(durationHours);

        Bill savedBill = saveBill(ticket, durationHours, baseAmount, null);
        log.info("Bill generated successfully. Invoice number: {}, Amount: {}",
                savedBill.getInvoiceNumber(), savedBill.getTotalAmount());
        return savedBill;
    }

    /**
     * Generate bill with slab-based pricing
     */
    @Transactional
    public Bill generateBillWithSlabPricing(Long ticketId) {
        log.info("Generating bill with slab pricing for ticket ID: {}", ticketId);
        ParkingTicket ticket = getBillableTicket(ticketId);

        BigDecimal durationHours = billableHours(ticket.getDurationMinutes());
        BigDecimal baseAmount = calculateSlabBasedAmount(durationHours, ticket.getParkingSpace().getRatePerHour());

        Bill savedBill = saveBill(ticket, durationHours, baseAmount, "Slab-based pricing applied");
        log.info("Bill with slab pricing generated successfully. Invoice number: {}", savedBill.getInvoiceNumber());
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

    private Bill saveBill(ParkingTicket ticket, BigDecimal durationHours, BigDecimal baseAmount, String notes) {
        BigDecimal base = baseAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = base.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = base.add(taxAmount);

        Bill bill = Bill.builder()
                .invoiceNumber(newInvoiceNumber())
                .user(ticket.getUser())
                .parkingTicket(ticket)
                .ratePerHour(ticket.getParkingSpace().getRatePerHour())
                .durationHours(durationHours)
                .baseAmount(base)
                .taxAmount(taxAmount)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(totalAmount)
                .status(BillingStatus.PENDING)
                .billingDate(LocalDateTime.now(clock))
                .notes(notes)
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
     * Hours to charge: every started hour counts, with a minimum of one hour
     * (so a stay of under a minute is still charged one unit).
     */
    static BigDecimal billableHours(long durationMinutes) {
        long hours = Math.max(1, (durationMinutes + 59) / 60);
        return BigDecimal.valueOf(hours);
    }

    /**
     * Calculate amount based on slab pricing
     * Slab structure:
     * 0-1 hour: Full rate
     * 1-3 hours: 90% of rate
     * 3-8 hours: 80% of rate
     * 8+ hours: 70% of rate
     */
    static BigDecimal calculateSlabBasedAmount(BigDecimal hours, BigDecimal ratePerHour) {
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal remaining = hours;

        // First hour - 100%
        if (remaining.compareTo(BigDecimal.ONE) > 0) {
            totalAmount = totalAmount.add(ratePerHour);
            remaining = remaining.subtract(BigDecimal.ONE);
        } else {
            return hours.multiply(ratePerHour);
        }

        // 1-3 hours - 90%
        BigDecimal slab2Hours = BigDecimal.valueOf(2);
        if (remaining.compareTo(slab2Hours) > 0) {
            totalAmount = totalAmount.add(slab2Hours.multiply(ratePerHour).multiply(new BigDecimal("0.90")));
            remaining = remaining.subtract(slab2Hours);
        } else if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            totalAmount = totalAmount.add(remaining.multiply(ratePerHour).multiply(new BigDecimal("0.90")));
            return totalAmount;
        }

        // 3-8 hours - 80%
        BigDecimal slab3Hours = BigDecimal.valueOf(5);
        if (remaining.compareTo(slab3Hours) > 0) {
            totalAmount = totalAmount.add(slab3Hours.multiply(ratePerHour).multiply(new BigDecimal("0.80")));
            remaining = remaining.subtract(slab3Hours);
        } else if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            totalAmount = totalAmount.add(remaining.multiply(ratePerHour).multiply(new BigDecimal("0.80")));
            return totalAmount;
        }

        // 8+ hours - 70%
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            totalAmount = totalAmount.add(remaining.multiply(ratePerHour).multiply(new BigDecimal("0.70")));
        }

        return totalAmount;
    }

    /**
     * Mark bill as paid
     */
    @Transactional
    public Bill markBillAsPaid(Long billId, String paymentMethod) {
        log.info("Marking bill ID: {} as paid", billId);
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new BadRequestException("paymentMethod is mandatory");
        }

        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillingStatus.PENDING) {
            throw new ConflictException("Bill cannot be paid. Current status: " + bill.getStatus());
        }

        bill.setStatus(BillingStatus.PAID);
        bill.setPaymentMethod(paymentMethod);
        bill.setPaymentDate(LocalDateTime.now(clock));

        Bill updatedBill = billRepository.save(bill);
        log.info("Bill marked as paid successfully");
        return updatedBill;
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
    public List<Bill> getUserBills(Long userId) {
        log.info("Fetching bills for user ID: {}", userId);
        return billRepository.findByUserUserId(userId);
    }

    /**
     * Get pending bills for user
     */
    public List<Bill> getUserPendingBills(Long userId) {
        log.info("Fetching pending bills for user ID: {}", userId);
        return billRepository.findByUserIdAndStatus(userId, BillingStatus.PENDING);
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

        Bill bill = getBillById(billId);
        if (bill.getStatus() != BillingStatus.PENDING) {
            throw new ConflictException("Discount can only be applied to pending bills");
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
    public List<Bill> getAllPendingBills() {
        log.info("Fetching all pending bills");
        return billRepository.findByStatusOrderByBillingDateDesc(BillingStatus.PENDING);
    }

    /**
     * Get total revenue
     */
    public BigDecimal getTotalRevenue() {
        log.info("Calculating total revenue");
        List<Bill> paidBills = billRepository.findByStatus(BillingStatus.PAID);
        return paidBills.stream()
                .map(Bill::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
