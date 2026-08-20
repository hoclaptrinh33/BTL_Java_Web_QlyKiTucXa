package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.CheckInOut;

public interface CheckInOutRepository extends JpaRepository<CheckInOut, Long> {
}
