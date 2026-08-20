package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.RenewalRequest;

public interface RenewalRequestRepository extends JpaRepository<RenewalRequest, Long> {
}
