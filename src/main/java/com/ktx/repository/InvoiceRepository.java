package com.ktx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Invoice;
import com.ktx.domain.enums.InvoiceStatus;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStudentIdOrderByDueDateDesc(Long studentId);

    Optional<Invoice> findByIdempotencyKey(String idempotencyKey);

    List<Invoice> findByContractIdOrderByDueDateDesc(Long contractId);

    boolean existsByStudentIdAndStatus(Long studentId, InvoiceStatus status);
}
