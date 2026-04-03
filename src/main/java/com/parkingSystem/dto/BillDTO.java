package com.parkingSystem.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
}

