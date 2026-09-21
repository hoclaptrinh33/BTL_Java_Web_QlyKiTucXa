package com.ktx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.InvoiceItem;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    List<InvoiceItem> findByInvoiceId(Long invoiceId);

    List<InvoiceItem> findByInvoiceIdOrderByIdAsc(Long invoiceId);
}
