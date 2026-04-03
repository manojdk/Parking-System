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
     * Mark bill as paid
     */
    Bill markBillAsPaid(Long billId, String paymentMethod);

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
