package com.ktx.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Invoice;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStudentIdOrderByDueDateDesc(Long studentId);

    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);

    List<Invoice> findByContractIdOrderByDueDateDesc(Long contractId);

    boolean existsByStudentIdAndStatus(Long studentId, InvoiceStatus status);

    boolean existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
            Long roomId, LocalDate billingMonth, InvoiceType invoiceType, InvoiceStatus status);

    List<Invoice> findByStatusInAndDueDateBefore(List<InvoiceStatus> statuses, LocalDate today);

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
}
