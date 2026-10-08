package com.ktx.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByInvoiceId(Long invoiceId);

    @Query("""
            SELECT p FROM Payment p
            LEFT JOIN FETCH p.recordedBy rb
            WHERE p.invoice.id = :invoiceId
            ORDER BY p.paidAt DESC
            """)
    List<Payment> findByInvoiceIdOrderByPaidAtDesc(@Param("invoiceId") Long invoiceId);

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

    @Query("SELECT p.invoice.id, COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.invoice.id IN :ids GROUP BY p.invoice.id")
    List<Object[]> sumByInvoiceIds(@Param("ids") Collection<Long> ids);

    @Query(value = """
            SELECT p FROM Payment p
            JOIN FETCH p.invoice i
            JOIN FETCH i.student s
            LEFT JOIN FETCH p.recordedBy rb
            LEFT JOIN FETCH i.room r
            LEFT JOIN FETCH r.building b
            ORDER BY p.paidAt DESC, p.id DESC
            """,
            countQuery = "SELECT COUNT(p) FROM Payment p")
    Page<Payment> findAllPage(Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p")
    BigDecimal sumAll();

    @Query(value = """
            SELECT p FROM Payment p
            JOIN FETCH p.invoice i
            JOIN FETCH i.student s
            LEFT JOIN FETCH p.recordedBy rb
            LEFT JOIN FETCH i.room r
            LEFT JOIN FETCH r.building b
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(i.invoiceNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(p.referenceNo, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY p.paidAt DESC, p.id DESC
            """,
            countQuery = """
            SELECT COUNT(p) FROM Payment p
            JOIN p.invoice i
            JOIN i.student s
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(i.invoiceNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(p.referenceNo, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Payment> searchPage(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM Payment p
            JOIN p.invoice i
            JOIN i.student s
            WHERE (:keyword IS NULL OR :keyword = ''
                   OR LOWER(i.invoiceNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(COALESCE(p.referenceNo, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    BigDecimal sumSearch(@Param("keyword") String keyword);
}
