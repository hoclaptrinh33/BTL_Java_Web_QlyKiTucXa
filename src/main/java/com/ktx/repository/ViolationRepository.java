package com.ktx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Violation;

public interface ViolationRepository extends JpaRepository<Violation, Long> {

    @Query("""
            SELECT v FROM Violation v
            JOIN FETCH v.student s
            JOIN FETCH s.user
            JOIN FETCH v.recordedBy
            ORDER BY v.occurredAt DESC, v.id DESC
            """)
    List<Violation> findAllWithDetails();

    @Query("""
            SELECT v FROM Violation v
            JOIN FETCH v.student s
            JOIN FETCH s.user
            JOIN FETCH v.recordedBy
            WHERE s.id = :studentId
            ORDER BY v.occurredAt DESC, v.id DESC
            """)
    List<Violation> findByStudentIdWithDetails(@Param("studentId") Long studentId);
}
