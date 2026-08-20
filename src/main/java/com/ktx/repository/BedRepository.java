package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Bed;

public interface BedRepository extends JpaRepository<Bed, Long> {
}
