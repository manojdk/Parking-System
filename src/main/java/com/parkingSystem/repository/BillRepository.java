package com.parkingSystem.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.parkingSystem.model.Bill;
import com.parkingSystem.enums.BillingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Bill entity
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByInvoiceNumber(String invoiceNumber);

    /**
     * Loads the bill and locks its row until the transaction ends. Every change to a bill's payment state
     * (online payment, webhook, cash payment, discount) goes through this, so they cannot race each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bill b WHERE b.billId = :id")
    Optional<Bill> findByIdForUpdate(@Param("id") Long billId);

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

    boolean existsByParkingTicketTicketId(Long ticketId);

    Optional<Bill> findByParkingTicketTicketId(Long ticketId);

    // ---- paginated (step 6)

    Page<Bill> findByUserUserId(Long userId, Pageable pageable);

    Page<Bill> findByUserUserIdAndStatus(Long userId, BillingStatus status, Pageable pageable);

    Page<Bill> findByStatus(BillingStatus status, Pageable pageable);

    /** Sum of the totals of all bills with the given status, computed by the database (0 when there are none). */
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.status = :status")
    java.math.BigDecimal sumTotalAmountByStatus(@Param("status") BillingStatus status);
}
