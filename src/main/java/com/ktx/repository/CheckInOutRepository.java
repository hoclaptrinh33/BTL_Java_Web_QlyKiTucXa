package com.ktx.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.CheckInOut;
import com.ktx.domain.enums.CheckInOutType;

public interface CheckInOutRepository extends JpaRepository<CheckInOut, Long> {

    @Query("""
            SELECT c FROM CheckInOut c
            JOIN FETCH c.performedBy
            WHERE c.contract.id = :contractId
            ORDER BY c.performedAt DESC
            """)
    List<CheckInOut> findByContractIdOrderByPerformedAtDesc(@Param("contractId") Long contractId);

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
    List<CheckInOut> findRecentWithLimit(@Param("buildingId") Long buildingId, Pageable pageable);

    @Query(value = """
            SELECT c FROM CheckInOut c
            JOIN FETCH c.contract ct
            JOIN FETCH ct.student s
            JOIN FETCH ct.bed b
            JOIN FETCH b.room r
            JOIN FETCH r.building build
            JOIN FETCH c.performedBy u
            WHERE (:buildingId IS NULL OR r.building.id = :buildingId)
              AND (:eventType IS NULL OR c.eventType = :eventType)
              AND (:ok IS NULL OR c.ok = :ok)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(ct.contractNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY c.performedAt DESC, c.id DESC
            """,
            countQuery = """
            SELECT COUNT(c) FROM CheckInOut c
            JOIN c.contract ct
            JOIN ct.student s
            JOIN ct.bed b
            JOIN b.room r
            WHERE (:buildingId IS NULL OR r.building.id = :buildingId)
              AND (:eventType IS NULL OR c.eventType = :eventType)
              AND (:ok IS NULL OR c.ok = :ok)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(ct.contractNo) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(r.roomNumber) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<CheckInOut> findPageWithDetails(@Param("buildingId") Long buildingId,
                                         @Param("eventType") CheckInOutType eventType,
                                         @Param("ok") Boolean ok,
                                         @Param("keyword") String keyword,
                                         Pageable pageable);

    long countByEventType(CheckInOutType eventType);

    long countByOk(Boolean ok);
}
