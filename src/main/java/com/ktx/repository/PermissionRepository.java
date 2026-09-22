package com.ktx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Permission;
import com.ktx.domain.enums.PermissionPlane;

public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByCode(String code);

    List<Permission> findByPlane(PermissionPlane plane);
}
