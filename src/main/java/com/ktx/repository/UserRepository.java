package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {
}
