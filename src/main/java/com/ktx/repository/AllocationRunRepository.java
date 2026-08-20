package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.AllocationRun;

public interface AllocationRunRepository extends JpaRepository<AllocationRun, Long> {
}
