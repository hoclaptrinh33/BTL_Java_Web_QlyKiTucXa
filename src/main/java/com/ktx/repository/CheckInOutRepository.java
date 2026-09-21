package com.ktx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.CheckInOut;
import com.ktx.domain.enums.CheckInOutType;

public interface CheckInOutRepository extends JpaRepository<CheckInOut, Long> {

    List<CheckInOut> findByContractIdOrderByPerformedAtDesc(Long contractId);

    boolean existsByContractIdAndEventType(Long contractId, CheckInOutType eventType);

    @Query("""
            SELECT c FROM CheckInOut c
            JOIN FETCH c.contract ct
            JOIN FETCH ct.student s
            JOIN FETCH ct.bed b
            JOIN FETCH b.room r
            JOIN FETCH r.building build
            JOIN FETCH c.performedBy u
            WHERE (:buildingId IS NULL OR r.building.id = :buildingId)
            ORDER BY c.performedAt DESC
            """)
    List<CheckInOut> findRecentWithDetails(@Param("buildingId") Long buildingId);
}
