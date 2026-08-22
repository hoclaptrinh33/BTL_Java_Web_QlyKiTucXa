package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Building;

public interface BuildingRepository extends JpaRepository<Building, Long> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, Long id);
}
