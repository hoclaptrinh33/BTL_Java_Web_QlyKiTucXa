package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.UtilityReading;

public interface UtilityReadingRepository extends JpaRepository<UtilityReading, Long> {
}
