package com.parkingSystem.controller;

import com.parkingSystem.pricing.PricingMode;
import org.springframework.data.domain.Sort;
import com.parkingSystem.dto.PageResponse;
import com.parkingSystem.controller.support.Pagination;
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
import java.util.Map;

/**
 * Controller for Billing and Invoice management
 */
@RestController
@RequestMapping("/api/billing")
@AllArgsConstructor
@Slf4j
public class BillingController {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "billingDate", "billId");

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
        Bill bill = billingService.generateBill(ticketId, PricingMode.HOURLY);

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
        Bill bill = billingService.generateBill(ticketId, PricingMode.SLAB);

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
    public PageResponse<BillDTO> getUserBills(@PathVariable Long userId,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        currentUserService.checkAccess(userId);
        return PageResponse.of(billingService.getUserBills(userId, Pagination.of(page, size, NEWEST_FIRST)),
                BillDTO::from);
    }

    /**
     * Get pending bills for a user
     */
    @GetMapping("/user/{userId}/pending")
    public PageResponse<BillDTO> getUserPendingBills(@PathVariable Long userId,
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        currentUserService.checkAccess(userId);
        return PageResponse.of(billingService.getUserPendingBills(userId, Pagination.of(page, size, NEWEST_FIRST)),
                BillDTO::from);
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
    public PageResponse<BillDTO> getAllPendingBills(
            @RequestParam(defaultValue = Pagination.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = Pagination.DEFAULT_SIZE) int size) {
        return PageResponse.of(billingService.getAllPendingBills(Pagination.of(page, size, NEWEST_FIRST)),
                BillDTO::from);
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
