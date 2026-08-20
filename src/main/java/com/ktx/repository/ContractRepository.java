package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Contract;

public interface ContractRepository extends JpaRepository<Contract, Long> {
}
