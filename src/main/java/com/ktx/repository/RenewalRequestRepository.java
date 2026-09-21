package com.ktx.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.RenewalRequest;
import com.ktx.domain.enums.RenewalStatus;

public interface RenewalRequestRepository extends JpaRepository<RenewalRequest, Long> {

    @Query("""
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.student s
            JOIN FETCH r.contract c
            LEFT JOIN FETCH c.bed b
            LEFT JOIN FETCH b.room rm
            LEFT JOIN FETCH rm.building bd
            WHERE r.id = :id
            """)
    Optional<RenewalRequest> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.student s
            JOIN FETCH r.contract c
            LEFT JOIN FETCH c.bed b
            LEFT JOIN FETCH b.room rm
            LEFT JOIN FETCH rm.building bd
            WHERE r.student.id = :studentId
            ORDER BY r.id DESC
            """)
    List<RenewalRequest> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("""
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.student s
            JOIN FETCH r.contract c
            LEFT JOIN FETCH c.bed b
            LEFT JOIN FETCH b.room rm
            LEFT JOIN FETCH rm.building bd
            WHERE r.contract.id = :contractId
            ORDER BY r.id DESC
            """)
    List<RenewalRequest> findByContractIdWithDetails(@Param("contractId") Long contractId);

    List<RenewalRequest> findByContractIdAndStatus(Long contractId, RenewalStatus status);

    boolean existsByStudentIdAndStatus(Long studentId, RenewalStatus status);

    boolean existsByContractIdAndStatus(Long contractId, RenewalStatus status);

    @Query("""
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.student s
            JOIN FETCH r.contract c
            LEFT JOIN FETCH c.bed b
            LEFT JOIN FETCH b.room rm
            LEFT JOIN FETCH rm.building bd
            WHERE (:status IS NULL OR r.status = :status)
              AND (:buildingId IS NULL OR bd.id = :buildingId)
            ORDER BY r.id DESC
            """)
    List<RenewalRequest> searchRequests(@Param("status") RenewalStatus status,
                                       @Param("buildingId") Long buildingId);

    @Query("""
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.contract c
            WHERE r.status = :status
              AND c.endDate < :date
            """)
    List<RenewalRequest> findPendingRenewalsWithContractEndDateBefore(@Param("status") RenewalStatus status,
                                                                     @Param("date") LocalDate date);
}

