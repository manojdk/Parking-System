package com.parkingSystem.service;

import com.parkingSystem.model.Bill;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for billing and invoice management
 */
public interface IBillingService {

    /**
     * Generate bill for a parking ticket (hourly billing)
     */
    Bill generateBill(Long ticketId);

    /**
     * Generate bill with slab-based pricing
     */
    Bill generateBillWithSlabPricing(Long ticketId);

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
    List<Bill> getUserBills(Long userId);

    /**
     * Get pending bills for user
     */
    List<Bill> getUserPendingBills(Long userId);

    /**
     * Apply discount to bill
     */
    Bill applyDiscount(Long billId, BigDecimal discountAmount);

    /**
     * Get all pending bills
     */
    List<Bill> getAllPendingBills();

    /**
     * Get total revenue
     */
    BigDecimal getTotalRevenue();
}
