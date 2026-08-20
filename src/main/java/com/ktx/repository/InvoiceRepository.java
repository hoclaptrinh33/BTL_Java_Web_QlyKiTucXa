package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
}
