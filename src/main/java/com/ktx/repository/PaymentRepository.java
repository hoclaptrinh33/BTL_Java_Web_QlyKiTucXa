package com.ktx.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceId(Long invoiceId);

    List<Payment> findByInvoiceIdOrderByPaidAtDesc(Long invoiceId);

    List<Payment> findAllByOrderByPaidAtDesc();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id = :invoiceId")
    BigDecimal sumAmountByInvoiceId(@Param("invoiceId") Long invoiceId);

    @Query("SELECT p FROM Payment p WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(p.invoice.invoiceNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.invoice.student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(p.invoice.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(COALESCE(p.referenceNo, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY p.paidAt DESC")
    List<Payment> searchPayments(@Param("keyword") String keyword);
}
