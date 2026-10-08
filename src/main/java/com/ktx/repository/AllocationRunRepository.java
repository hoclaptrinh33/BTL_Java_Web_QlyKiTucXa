package com.ktx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.AllocationRun;
import com.ktx.domain.enums.AllocationRunStatus;

public interface AllocationRunRepository extends JpaRepository<AllocationRun, Long> {
    boolean existsByPeriodId(Long periodId);
    List<AllocationRun> findByPeriodIdOrderByIdDesc(Long periodId);
    Optional<AllocationRun> findTopByPeriodIdAndStatusOrderByIdDesc(Long periodId, AllocationRunStatus status);
    Optional<AllocationRun> findTopByPeriodIdOrderByIdDesc(Long periodId);

    @Query("SELECT r FROM AllocationRun r " +
           "JOIN FETCH r.period p " +
           "LEFT JOIN FETCH r.runBy u " +
           "WHERE r.id = :id")
    Optional<AllocationRun> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT r FROM AllocationRun r " +
           "JOIN FETCH r.period p " +
           "LEFT JOIN FETCH r.runBy u " +
           "WHERE r.period.id = :periodId ORDER BY r.id DESC")
    List<AllocationRun> findByPeriodIdWithDetailsOrderByIdDesc(@Param("periodId") Long periodId);
}
