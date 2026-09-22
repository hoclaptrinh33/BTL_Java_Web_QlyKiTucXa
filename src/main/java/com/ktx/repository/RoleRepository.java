package com.ktx.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByCode(String code);

    boolean existsByCode(String code);
}
