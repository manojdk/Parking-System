package com.parkingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.parkingSystem.model.Bill;
import java.math.BigDecimal;

/**
 * DTO for Bill response
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BillDTO {
    private Long billId;
    private String invoiceNumber;
    private Long userId;
    private Long ticketId;
    private BigDecimal ratePerHour;
    private BigDecimal durationHours;
    private BigDecimal baseAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;

    public static BillDTO from(Bill bill) {
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

