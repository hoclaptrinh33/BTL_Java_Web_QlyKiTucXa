package com.ktx.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.UtilityReading;

public interface UtilityReadingRepository extends JpaRepository<UtilityReading, Long> {

    Optional<UtilityReading> findByRoomIdAndBillingMonth(Long roomId, LocalDate billingMonth);

    boolean existsByRoomIdAndBillingMonth(Long roomId, LocalDate billingMonth);

    List<UtilityReading> findByRoomIdOrderByBillingMonthDesc(Long roomId);

    Optional<UtilityReading> findTopByRoomIdOrderByBillingMonthDesc(Long roomId);

    @Query("""
            SELECT u FROM UtilityReading u
            JOIN FETCH u.room r
            JOIN FETCH r.building b
            WHERE b.id = :buildingId
              AND u.billingMonth = :billingMonth
            ORDER BY r.roomNumber ASC
            """)
    List<UtilityReading> findByBuildingIdAndBillingMonth(
            @Param("buildingId") Long buildingId,
            @Param("billingMonth") LocalDate billingMonth);

    @Query("""
            SELECT u FROM UtilityReading u
            JOIN FETCH u.room r
            JOIN FETCH r.building b
            WHERE u.billingMonth = :billingMonth
            ORDER BY b.code ASC, r.roomNumber ASC
            """)
    List<UtilityReading> findByBillingMonth(
            @Param("billingMonth") LocalDate billingMonth);
}
