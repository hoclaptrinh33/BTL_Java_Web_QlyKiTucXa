package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.SystemLock;

public interface SystemLockRepository extends JpaRepository<SystemLock, String> {
}
