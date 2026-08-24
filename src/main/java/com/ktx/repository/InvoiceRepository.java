package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import com.ktx.domain.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByStudentIdOrderByDueDateDesc(Long studentId);
}
