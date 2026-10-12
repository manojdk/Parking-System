package com.parkingSystem.controller;

import com.parkingSystem.exception.ResourceNotFoundException;
import com.parkingSystem.security.CurrentUserService;
import com.parkingSystem.service.IBillingService;
import com.parkingSystem.model.Bill;
import com.parkingSystem.dto.BillDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for Billing and Invoice management
 */
@RestController
@RequestMapping("/api/billing")
@AllArgsConstructor
@Slf4j
public class BillingController {

    private IBillingService billingService;
    private CurrentUserService currentUserService;

    /**
     * Generate bill for a parking ticket (hourly pricing). Bills are normally created
     * automatically on vehicle exit; this endpoint is for admins.
     */
    @PostMapping("/generate-hourly/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateBillHourly(@PathVariable Long ticketId) {
        log.info("Generating hourly bill for ticket ID: {}", ticketId);
        Bill bill = billingService.generateBill(ticketId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Bill generated successfully");
        response.put("bill", BillDTO.from(bill));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Generate bill for a parking ticket (slab-based pricing)
     */
    @PostMapping("/generate-slab/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> generateBillSlab(@PathVariable Long ticketId) {
        log.info("Generating slab-based bill for ticket ID: {}", ticketId);
        Bill bill = billingService.generateBillWithSlabPricing(ticketId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Bill generated successfully with slab-based pricing");
        response.put("bill", BillDTO.from(bill));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get bill by invoice number
     */
    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<?> getBillByInvoice(@PathVariable String invoiceNumber) {
        Bill bill = billingService.getBillByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found"));
        currentUserService.checkAccess(bill.getUser().getUserId());
        return ResponseEntity.ok(BillDTO.from(bill));
    }

    /**
     * Get all bills for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserBills(@PathVariable Long userId) {
        currentUserService.checkAccess(userId);
        List<Bill> bills = billingService.getUserBills(userId);
        List<BillDTO> dtos = bills.stream().map(BillDTO::from).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("count", bills.size());
        response.put("bills", dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * Get pending bills for a user
     */
    @GetMapping("/user/{userId}/pending")
    public ResponseEntity<?> getUserPendingBills(@PathVariable Long userId) {
        currentUserService.checkAccess(userId);
        List<Bill> pendingBills = billingService.getUserPendingBills(userId);
        List<BillDTO> dtos = pendingBills.stream().map(BillDTO::from).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("pendingCount", pendingBills.size());
        response.put("bills", dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * Record an offline payment, e.g. cash at the counter (admin only). Users pay online through
     * {@code POST /api/payments}; they can no longer mark their own bills as paid.
     */
    @PutMapping("/{billId}/pay")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> markBillAsPaid(@PathVariable Long billId,
                                            @RequestParam String paymentMethod) {
        log.info("Recording offline payment for bill ID: {}", billId);
        Bill bill = billingService.markBillAsPaid(billId, paymentMethod);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Bill marked as paid successfully");
        response.put("bill", BillDTO.from(bill));
        return ResponseEntity.ok(response);
    }

    /**
     * Apply discount to bill (admin only)
     */
    @PutMapping("/{billId}/apply-discount")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> applyDiscount(@PathVariable Long billId,
                                           @RequestParam BigDecimal discountAmount) {
        log.info("Applying discount to bill ID: {}", billId);
        Bill bill = billingService.applyDiscount(billId, discountAmount);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Discount applied successfully");
        response.put("bill", BillDTO.from(bill));
        return ResponseEntity.ok(response);
    }

    /**
     * Get all pending bills (admin view)
     */
    @GetMapping("/pending-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllPendingBills() {
        List<Bill> pendingBills = billingService.getAllPendingBills();
        List<BillDTO> dtos = pendingBills.stream().map(BillDTO::from).collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalPending", pendingBills.size());
        response.put("bills", dtos);
        return ResponseEntity.ok(response);
    }

    /**
     * Get total revenue (admin view)
     */
    @GetMapping("/revenue-total")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getTotalRevenue() {
        BigDecimal totalRevenue = billingService.getTotalRevenue();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("totalRevenue", totalRevenue);
        return ResponseEntity.ok(response);
    }
}
