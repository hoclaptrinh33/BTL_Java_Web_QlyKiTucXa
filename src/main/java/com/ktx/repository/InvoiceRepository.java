package com.ktx.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Invoice;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStudentIdOrderByDueDateDesc(Long studentId);

    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);

    @Query("""
            SELECT i FROM Invoice i
            JOIN FETCH i.student s
            LEFT JOIN FETCH i.room r
            LEFT JOIN FETCH r.building b
            WHERE i.id = :id
            """)
    Optional<Invoice> findByIdWithDetails(@Param("id") Long id);

    List<Invoice> findByContractIdOrderByDueDateDesc(Long contractId);

    boolean existsByStudentIdAndStatus(Long studentId, InvoiceStatus status);

    boolean existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
            Long roomId, LocalDate billingMonth, InvoiceType invoiceType, InvoiceStatus status);

    List<Invoice> findByStatusInAndDueDateBefore(List<InvoiceStatus> statuses, LocalDate today);

    @Query("""
            SELECT i FROM Invoice i
            WHERE i.dueDate < :today
              AND i.status IN :statuses
              AND (i.lateFee IS NULL OR i.lateFee = 0)
            """)
    List<Invoice> findNeedingLateFee(@Param("statuses") Collection<InvoiceStatus> statuses,
                                     @Param("today") LocalDate today);

    @Query("""
            SELECT YEAR(COALESCE(i.billingMonth, i.dueDate)),
                   MONTH(COALESCE(i.billingMonth, i.dueDate)),
                   COALESCE(SUM(i.total), 0),
                   COUNT(i)
            FROM Invoice i
            WHERE i.status IN :statuses
            GROUP BY YEAR(COALESCE(i.billingMonth, i.dueDate)),
                     MONTH(COALESCE(i.billingMonth, i.dueDate))
            ORDER BY YEAR(COALESCE(i.billingMonth, i.dueDate)),
                     MONTH(COALESCE(i.billingMonth, i.dueDate))
            """)
    List<Object[]> sumDebtByMonth(@Param("statuses") Collection<InvoiceStatus> statuses);

    @Query("SELECT i.status, COUNT(i), COALESCE(SUM(i.total), 0) FROM Invoice i GROUP BY i.status")
    List<Object[]> summarizeByStatus();

    List<Invoice> findByStatusIn(java.util.Collection<InvoiceStatus> statuses);

    List<Invoice> findByStatusInOrderByDueDateAsc(java.util.Collection<InvoiceStatus> statuses);

    long countByStatus(InvoiceStatus status);

    List<Invoice> findAllByOrderByDueDateDesc();

    @Query("SELECT i FROM Invoice i WHERE " +
            "(:status IS NULL OR i.status = :status) AND " +
            "(:invoiceType IS NULL OR i.invoiceType = :invoiceType) AND " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            " LOWER(i.invoiceNo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(i.student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(i.student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " (i.room IS NOT NULL AND LOWER(i.room.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
            "ORDER BY i.dueDate DESC")
    List<Invoice> searchInvoices(@Param("status") InvoiceStatus status,
                                 @Param("invoiceType") InvoiceType invoiceType,
                                 @Param("keyword") String keyword);

    @Query(value = """
            SELECT i FROM Invoice i
            JOIN FETCH i.student s
            LEFT JOIN FETCH i.room r
            LEFT JOIN FETCH r.building b
            WHERE (:status IS NULL OR i.status = :status)
              AND (:invoiceType IS NULL OR i.invoiceType = :invoiceType)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(i.invoiceNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR (r IS NOT NULL AND LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))))
            ORDER BY i.dueDate DESC, i.id DESC
            """,
            countQuery = """
            SELECT COUNT(i) FROM Invoice i
            JOIN i.student s
            LEFT JOIN i.room r
            WHERE (:status IS NULL OR i.status = :status)
              AND (:invoiceType IS NULL OR i.invoiceType = :invoiceType)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(i.invoiceNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR (r IS NOT NULL AND LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))))
            """)
    Page<Invoice> searchPage(@Param("status") InvoiceStatus status,
                             @Param("invoiceType") InvoiceType invoiceType,
                             @Param("keyword") String keyword,
                             Pageable pageable);

    @Query(value = """
            SELECT i FROM Invoice i
            JOIN FETCH i.student s
            LEFT JOIN FETCH i.room r
            LEFT JOIN FETCH r.building b
            ORDER BY i.dueDate DESC, i.id DESC
            """,
            countQuery = "SELECT COUNT(i) FROM Invoice i")
    Page<Invoice> findAllPage(Pageable pageable);

    @Query("""
            SELECT i.room.id FROM Invoice i
            WHERE i.room.building.id = :buildingId
              AND i.billingMonth = :billingMonth
              AND i.invoiceType = :invoiceType
              AND i.status != :status
            """)
    List<Long> findRoomIdsWithActiveInvoice(
            @Param("buildingId") Long buildingId,
            @Param("billingMonth") LocalDate billingMonth,
            @Param("invoiceType") InvoiceType invoiceType,
            @Param("status") InvoiceStatus status);
}
