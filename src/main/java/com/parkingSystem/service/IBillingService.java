package com.parkingSystem.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.parkingSystem.model.Bill;
import com.parkingSystem.pricing.PricingMode;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * Service interface for billing and invoice management
 */
public interface IBillingService {

    /**
     * Bill a closed ticket with the configured pricing mode ({@code app.pricing.mode}); used on vehicle exit
     */
    Bill generateBill(Long ticketId);

    /**
     * Bill a closed, not yet billed ticket with the given pricing mode
     */
    Bill generateBill(Long ticketId, PricingMode mode);

    /**
     * Record a payment made outside the app (for example cash at the counter). Admin only; refused while an
     * online payment for the bill is in progress, so the bill can never be paid twice.
     */
    Bill markBillAsPaid(Long billId, String paymentMethod);

    /**
     * Get bill by ID
     *
     * @throws com.parkingSystem.exception.ResourceNotFoundException if it does not exist
     */
    Bill getBillById(Long billId);

    /**
     * Get the bill generated for a ticket, if any
     */
    Optional<Bill> getBillForTicket(Long ticketId);

    /**
     * Get bill by invoice number
     */
    Optional<Bill> getBillByInvoiceNumber(String invoiceNumber);

    /**
     * Get user's bills
     */
    Page<Bill> getUserBills(Long userId, Pageable pageable);

    /**
     * Get pending bills for user
     */
    Page<Bill> getUserPendingBills(Long userId, Pageable pageable);

    /**
     * Apply discount to bill
     */
    Bill applyDiscount(Long billId, BigDecimal discountAmount);

    /**
     * Get all pending bills
     */
    Page<Bill> getAllPendingBills(Pageable pageable);

    /**
     * Get total revenue
     */
    BigDecimal getTotalRevenue();
}
