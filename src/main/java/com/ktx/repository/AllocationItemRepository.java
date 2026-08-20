package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.AllocationItem;

public interface AllocationItemRepository extends JpaRepository<AllocationItem, Long> {
}
