package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Violation;

public interface ViolationRepository extends JpaRepository<Violation, Long> {
}
