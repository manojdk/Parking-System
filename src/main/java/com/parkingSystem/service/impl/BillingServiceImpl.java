package com.parkingSystem.service.impl;

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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for billing and invoice management
 */
@Service
@AllArgsConstructor
@Slf4j
public class BillingServiceImpl implements com.parkingSystem.service.IBillingService {

    private BillRepository billRepository;
    private ParkingTicketRepository parkingTicketRepository;

    /**
     * Generate bill for a parking ticket (hourly billing)
     */
    @Transactional
    public Bill generateBill(Long ticketId) {
        try {
            log.info("Generating bill for ticket ID: {}", ticketId);

            ParkingTicket ticket = parkingTicketRepository.findById(ticketId)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

            // Check if bill already exists
            Optional<Bill> existingBill = billRepository.findAll().stream()
                    .filter(b -> b.getParkingTicket().getTicketId().equals(ticketId))
                    .findFirst();

            if (existingBill.isPresent()) {
                throw new IllegalArgumentException("Bill already exists for this ticket");
            }

            // Calculate duration and amount
            Long durationMinutes = ticket.getDurationMinutes();
            if (durationMinutes == null) {
                throw new IllegalArgumentException("Parking duration not set. Vehicle hasn't exited yet.");
            }

            // Convert to hours (round up)
            BigDecimal durationHours = BigDecimal.valueOf(Math.ceil(durationMinutes / 60.0));
            BigDecimal ratePerHour = ticket.getParkingSpace().getRatePerHour();

            // Calculate base amount
            BigDecimal baseAmount = ratePerHour.multiply(durationHours);

            // Calculate tax (10% by default)
            BigDecimal taxAmount = baseAmount.multiply(BigDecimal.valueOf(0.10));

            // Total amount
            BigDecimal totalAmount = baseAmount.add(taxAmount);

            // Create bill
            Bill bill = Bill.builder()
                    .user(ticket.getUser())
                    .parkingTicket(ticket)
                    .ratePerHour(ratePerHour)
                    .durationHours(durationHours)
                    .baseAmount(baseAmount)
                    .taxAmount(taxAmount)
                    .totalAmount(totalAmount)
                    .status(BillingStatus.PENDING)
                    .build();

            Bill savedBill = billRepository.save(bill);
            log.info("Bill generated successfully. Invoice number: {}, Amount: {}", 
                    savedBill.getInvoiceNumber(), savedBill.getTotalAmount());
            return savedBill;
        } catch (Exception e) {
            log.error("Error generating bill: {}", e.getMessage());
            throw new RuntimeException("Error generating bill: " + e.getMessage());
        }
    }

    /**
     * Generate bill with slab-based pricing
     */
    @Transactional
    public Bill generateBillWithSlabPricing(Long ticketId) {
        try {
            log.info("Generating bill with slab pricing for ticket ID: {}", ticketId);

            ParkingTicket ticket = parkingTicketRepository.findById(ticketId)
                    .orElseThrow(() -> new IllegalArgumentException("Ticket not found"));

            Long durationMinutes = ticket.getDurationMinutes();
            if (durationMinutes == null) {
                throw new IllegalArgumentException("Parking duration not set.");
            }

            BigDecimal durationHours = BigDecimal.valueOf(Math.ceil(durationMinutes / 60.0));
            BigDecimal baseAmount = calculateSlabBasedAmount(durationHours, ticket.getParkingSpace().getRatePerHour());

            BigDecimal taxAmount = baseAmount.multiply(BigDecimal.valueOf(0.10));
            BigDecimal totalAmount = baseAmount.add(taxAmount);

            Bill bill = Bill.builder()
                    .user(ticket.getUser())
                    .parkingTicket(ticket)
                    .ratePerHour(ticket.getParkingSpace().getRatePerHour())
                    .durationHours(durationHours)
                    .baseAmount(baseAmount)
                    .taxAmount(taxAmount)
                    .totalAmount(totalAmount)
                    .status(BillingStatus.PENDING)
                    .notes("Slab-based pricing applied")
                    .build();

            Bill savedBill = billRepository.save(bill);
            log.info("Bill with slab pricing generated successfully");
            return savedBill;
        } catch (Exception e) {
            log.error("Error generating bill with slab pricing: {}", e.getMessage());
            throw new RuntimeException("Error generating bill: " + e.getMessage());
        }
    }

    /**
     * Calculate amount based on slab pricing
     * Slab structure:
     * 0-1 hour: Full rate
     * 1-3 hours: 90% of rate
     * 3-8 hours: 80% of rate
     * 8+ hours: 70% of rate
     */
    private BigDecimal calculateSlabBasedAmount(BigDecimal hours, BigDecimal ratePerHour) {
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
            totalAmount = totalAmount.add(slab2Hours.multiply(ratePerHour).multiply(BigDecimal.valueOf(0.90)));
            remaining = remaining.subtract(slab2Hours);
        } else if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            totalAmount = totalAmount.add(remaining.multiply(ratePerHour).multiply(BigDecimal.valueOf(0.90)));
            return totalAmount;
        }

        // 3-8 hours - 80%
        BigDecimal slab3Hours = BigDecimal.valueOf(5);
        if (remaining.compareTo(slab3Hours) > 0) {
            totalAmount = totalAmount.add(slab3Hours.multiply(ratePerHour).multiply(BigDecimal.valueOf(0.80)));
            remaining = remaining.subtract(slab3Hours);
        } else if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            totalAmount = totalAmount.add(remaining.multiply(ratePerHour).multiply(BigDecimal.valueOf(0.80)));
            return totalAmount;
        }

        // 8+ hours - 70%
        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            totalAmount = totalAmount.add(remaining.multiply(ratePerHour).multiply(BigDecimal.valueOf(0.70)));
        }

        return totalAmount;
    }

    /**
     * Mark bill as paid
     */
    @Transactional
    public Bill markBillAsPaid(Long billId, String paymentMethod) {
        try {
            log.info("Marking bill ID: {} as paid", billId);

            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

            bill.setStatus(BillingStatus.PAID);
            bill.setPaymentMethod(paymentMethod);
            bill.setPaymentDate(LocalDateTime.now());

            Bill updatedBill = billRepository.save(bill);
            log.info("Bill marked as paid successfully");
            return updatedBill;
        } catch (Exception e) {
            log.error("Error marking bill as paid: {}", e.getMessage());
            throw new RuntimeException("Error marking bill as paid: " + e.getMessage());
        }
    }

    /**
     * Get bill by invoice number
     */
    public Optional<Bill> getBillByInvoiceNumber(String invoiceNumber) {
        try {
            log.info("Fetching bill by invoice number: {}", invoiceNumber);
            return billRepository.findByInvoiceNumber(invoiceNumber);
        } catch (Exception e) {
            log.error("Error fetching bill: {}", e.getMessage());
            throw new RuntimeException("Error fetching bill: " + e.getMessage());
        }
    }

    /**
     * Get user's bills
     */
    public List<Bill> getUserBills(Long userId) {
        try {
            log.info("Fetching bills for user ID: {}", userId);
            return billRepository.findByUserUserId(userId);
        } catch (Exception e) {
            log.error("Error fetching user bills: {}", e.getMessage());
            throw new RuntimeException("Error fetching user bills: " + e.getMessage());
        }
    }

    /**
     * Get pending bills for user
     */
    public List<Bill> getUserPendingBills(Long userId) {
        try {
            log.info("Fetching pending bills for user ID: {}", userId);
            return billRepository.findByUserIdAndStatus(userId, BillingStatus.PENDING);
        } catch (Exception e) {
            log.error("Error fetching pending bills: {}", e.getMessage());
            throw new RuntimeException("Error fetching pending bills: " + e.getMessage());
        }
    }

    /**
     * Apply discount to bill
     */
    @Transactional
    public Bill applyDiscount(Long billId, BigDecimal discountAmount) {
        try {
            log.info("Applying discount to bill ID: {}", billId);

            Bill bill = billRepository.findById(billId)
                    .orElseThrow(() -> new IllegalArgumentException("Bill not found"));

            if (bill.getStatus() == BillingStatus.PAID) {
                throw new IllegalArgumentException("Cannot apply discount to paid bill");
            }

            bill.setDiscountAmount(discountAmount);
            bill.setTotalAmount(bill.getBaseAmount().add(bill.getTaxAmount()).subtract(discountAmount));

            Bill updatedBill = billRepository.save(bill);
            log.info("Discount applied successfully");
            return updatedBill;
        } catch (Exception e) {
            log.error("Error applying discount: {}", e.getMessage());
            throw new RuntimeException("Error applying discount: " + e.getMessage());
        }
    }

    /**
     * Get all pending bills
     */
    public List<Bill> getAllPendingBills() {
        try {
            log.info("Fetching all pending bills");
            return billRepository.findByStatusOrderByBillingDateDesc(BillingStatus.PENDING);
        } catch (Exception e) {
            log.error("Error fetching pending bills: {}", e.getMessage());
            throw new RuntimeException("Error fetching pending bills: " + e.getMessage());
        }
    }

    /**
     * Get total revenue
     */
    public BigDecimal getTotalRevenue() {
        try {
            log.info("Calculating total revenue");
            List<Bill> paidBills = billRepository.findByStatus(BillingStatus.PAID);
            return paidBills.stream()
                    .map(Bill::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.error("Error calculating revenue: {}", e.getMessage());
            throw new RuntimeException("Error calculating revenue: " + e.getMessage());
        }
    }
}
