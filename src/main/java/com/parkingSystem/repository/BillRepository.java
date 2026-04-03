package com.parkingSystem.repository;

import com.parkingSystem.model.Bill;
import com.parkingSystem.enums.BillingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Bill entity
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByInvoiceNumber(String invoiceNumber);

    List<Bill> findByUserUserId(Long userId);

    List<Bill> findByStatus(BillingStatus status);

    @Query("SELECT b FROM Bill b WHERE b.user.userId = ?1 AND b.status = ?2")
    List<Bill> findByUserIdAndStatus(Long userId, BillingStatus status);

    @Query("SELECT b FROM Bill b WHERE b.billingDate BETWEEN ?1 AND ?2")
    List<Bill> findBillsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT b FROM Bill b WHERE b.user.userId = ?1 AND b.billingDate BETWEEN ?2 AND ?3")
    List<Bill> findUserBillsByDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate);

    List<Bill> findByStatusOrderByBillingDateDesc(BillingStatus status);

    Long countByStatus(BillingStatus status);
}

