package com.ktx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.AllocationRun;
import com.ktx.domain.enums.AllocationRunStatus;

public interface AllocationRunRepository extends JpaRepository<AllocationRun, Long> {
    boolean existsByPeriodId(Long periodId);
    List<AllocationRun> findByPeriodIdOrderByIdDesc(Long periodId);
    Optional<AllocationRun> findTopByPeriodIdAndStatusOrderByIdDesc(Long periodId, AllocationRunStatus status);
    Optional<AllocationRun> findTopByPeriodIdOrderByIdDesc(Long periodId);
}
