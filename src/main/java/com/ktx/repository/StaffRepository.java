package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Staff;

public interface StaffRepository extends JpaRepository<Staff, Long> {
}
