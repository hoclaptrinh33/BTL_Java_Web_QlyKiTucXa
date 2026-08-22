package com.ktx.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Staff;

public interface StaffRepository extends JpaRepository<Staff, Long> {

    @Query("""
            SELECT s FROM Staff s
            JOIN FETCH s.user
            JOIN FETCH s.assignedBuilding
            WHERE s.user.username = :username
            """)
    Optional<Staff> findByUserUsername(@Param("username") String username);

    @Query("""
            SELECT s FROM Staff s
            JOIN FETCH s.user
            JOIN FETCH s.assignedBuilding
            """)
    java.util.List<Staff> findAllWithUserAndBuilding();

    Optional<Staff> findByUserId(Long userId);
}
