package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
}
