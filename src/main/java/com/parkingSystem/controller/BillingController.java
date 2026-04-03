package com.parkingSystem.controller;

import com.parkingSystem.service.IBillingService;
import com.parkingSystem.model.Bill;
import com.parkingSystem.dto.BillDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    /**
     * Generate bill for a parking ticket (hourly pricing)
     */
    @PostMapping("/generate-hourly/{ticketId}")
    public ResponseEntity<?> generateBillHourly(@PathVariable Long ticketId) {
        try {
            log.info("Generating hourly bill for ticket ID: {}", ticketId);
            Bill bill = billingService.generateBill(ticketId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Bill generated successfully");
            response.put("bill", convertToDTO(bill));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error generating bill: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Generate bill for a parking ticket (slab-based pricing)
     */
    @PostMapping("/generate-slab/{ticketId}")
    public ResponseEntity<?> generateBillSlab(@PathVariable Long ticketId) {
        try {
            log.info("Generating slab-based bill for ticket ID: {}", ticketId);
            Bill bill = billingService.generateBillWithSlabPricing(ticketId);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Bill generated successfully with slab-based pricing");
            response.put("bill", convertToDTO(bill));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Error generating bill with slab pricing: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get bill by invoice number
     */
    @GetMapping("/invoice/{invoiceNumber}")
    public ResponseEntity<?> getBillByInvoice(@PathVariable String invoiceNumber) {
        try {
            Bill bill = billingService.getBillByInvoiceNumber(invoiceNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Bill not found"));
            return ResponseEntity.ok(convertToDTO(bill));
        } catch (Exception e) {
            log.error("Error fetching bill: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get all bills for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserBills(@PathVariable Long userId) {
        try {
            List<Bill> bills = billingService.getUserBills(userId);
            List<BillDTO> dtos = bills.stream().map(this::convertToDTO).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("count", bills.size());
            response.put("bills", dtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching user bills: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get pending bills for a user
     */
    @GetMapping("/user/{userId}/pending")
    public ResponseEntity<?> getUserPendingBills(@PathVariable Long userId) {
        try {
            List<Bill> pendingBills = billingService.getUserPendingBills(userId);
            List<BillDTO> dtos = pendingBills.stream().map(this::convertToDTO).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("pendingCount", pendingBills.size());
            response.put("bills", dtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching pending bills: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Mark bill as paid
     */
    @PutMapping("/{billId}/pay")
    public ResponseEntity<?> markBillAsPaid(@PathVariable Long billId, 
                                            @RequestParam String paymentMethod) {
        try {
            log.info("Marking bill ID: {} as paid", billId);
            Bill bill = billingService.markBillAsPaid(billId, paymentMethod);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Bill marked as paid successfully");
            response.put("bill", convertToDTO(bill));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error marking bill as paid: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Apply discount to bill
     */
    @PutMapping("/{billId}/apply-discount")
    public ResponseEntity<?> applyDiscount(@PathVariable Long billId, 
                                           @RequestParam BigDecimal discountAmount) {
        try {
            log.info("Applying discount to bill ID: {}", billId);
            Bill bill = billingService.applyDiscount(billId, discountAmount);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Discount applied successfully");
            response.put("bill", convertToDTO(bill));
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error applying discount: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get all pending bills (admin view)
     */
    @GetMapping("/pending-all")
    public ResponseEntity<?> getAllPendingBills() {
        try {
            List<Bill> pendingBills = billingService.getAllPendingBills();
            List<BillDTO> dtos = pendingBills.stream().map(this::convertToDTO).collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalPending", pendingBills.size());
            response.put("bills", dtos);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching all pending bills: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Get total revenue (admin view)
     */
    @GetMapping("/revenue-total")
    public ResponseEntity<?> getTotalRevenue() {
        try {
            BigDecimal totalRevenue = billingService.getTotalRevenue();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("totalRevenue", totalRevenue);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error calculating revenue: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Helper method to convert Bill to DTO
     */
    private BillDTO convertToDTO(Bill bill) {
        return BillDTO.builder()
                .billId(bill.getBillId())
                .invoiceNumber(bill.getInvoiceNumber())
                .userId(bill.getUser().getUserId())
                .ticketId(bill.getParkingTicket().getTicketId())
                .ratePerHour(bill.getRatePerHour())
                .durationHours(bill.getDurationHours())
                .baseAmount(bill.getBaseAmount())
                .taxAmount(bill.getTaxAmount())
                .discountAmount(bill.getDiscountAmount())
                .totalAmount(bill.getTotalAmount())
                .status(bill.getStatus().name())
                .paymentMethod(bill.getPaymentMethod())
                .build();
    }
}
