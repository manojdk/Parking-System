package com.parkingSystem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.parkingSystem.model.Payment;

import jakarta.persistence.LockModeType;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByUserUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /** The bill's payment in progress (PENDING), if any. */
    Optional<Payment> findByActiveBillId(Long billId);

    Optional<Payment> findByProviderReference(String providerReference);

    List<Payment> findByBillBillIdOrderByCreatedAtDesc(Long billId);

    /** The bill a provider reference belongs to, read without loading the payment (lock the bill first). */
    @Query("SELECT p.bill.billId FROM Payment p WHERE p.providerReference = :reference")
    Optional<Long> findBillIdByProviderReference(@Param("reference") String providerReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.providerReference = :reference")
    Optional<Payment> findByProviderReferenceForUpdate(@Param("reference") String providerReference);
}
