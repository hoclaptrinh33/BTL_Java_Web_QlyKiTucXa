package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.SystemConfig;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
}
