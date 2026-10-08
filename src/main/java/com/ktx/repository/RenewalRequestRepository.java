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
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            LEFT JOIN FETCH c.bed b
            LEFT JOIN FETCH b.room rm
            LEFT JOIN FETCH rm.building bd
            WHERE r.id = :id
            """)
    Optional<RenewalRequest> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            LEFT JOIN FETCH c.bed b
            LEFT JOIN FETCH b.room rm
            LEFT JOIN FETCH rm.building bd
            WHERE s.id = :studentId
            ORDER BY r.id DESC
            """)
    List<RenewalRequest> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("""
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            LEFT JOIN FETCH c.bed b
            LEFT JOIN FETCH b.room rm
            LEFT JOIN FETCH rm.building bd
            WHERE r.contract.id = :contractId
            ORDER BY r.id DESC
            """)
    List<RenewalRequest> findByContractIdWithDetails(@Param("contractId") Long contractId);

    List<RenewalRequest> findByContractIdAndStatus(Long contractId, RenewalStatus status);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RenewalRequest r
            WHERE r.contract.student.id = :studentId
              AND r.status = :status
            """)
    boolean existsByStudentIdAndStatus(@Param("studentId") Long studentId,
                                      @Param("status") RenewalStatus status);

    boolean existsByContractIdAndStatus(Long contractId, RenewalStatus status);

    @Query("""
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            LEFT JOIN FETCH c.bed b
            LEFT JOIN FETCH b.room rm
            LEFT JOIN FETCH rm.building bd
            WHERE (:status IS NULL OR r.status = :status)
              AND (:buildingId IS NULL OR bd.id = :buildingId)
            ORDER BY r.id DESC
            """)
    List<RenewalRequest> searchRequests(@Param("status") RenewalStatus status,
                                       @Param("buildingId") Long buildingId);

    @Query(value = """
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            LEFT JOIN FETCH c.bed b
            LEFT JOIN FETCH b.room rm
            LEFT JOIN FETCH rm.building bd
            WHERE (:status IS NULL OR r.status = :status)
              AND (:buildingId IS NULL OR bd.id = :buildingId)
            ORDER BY r.id DESC
            """,
           countQuery = """
            SELECT COUNT(r) FROM RenewalRequest r
            LEFT JOIN r.contract c
            LEFT JOIN c.bed b
            LEFT JOIN b.room rm
            LEFT JOIN rm.building bd
            WHERE (:status IS NULL OR r.status = :status)
              AND (:buildingId IS NULL OR bd.id = :buildingId)
            """)
    org.springframework.data.domain.Page<RenewalRequest> searchRequests(@Param("status") RenewalStatus status,
                                                                       @Param("buildingId") Long buildingId,
                                                                       org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT r FROM RenewalRequest r
            JOIN FETCH r.contract c
            WHERE r.status = :status
              AND c.endDate < :date
            """)
    List<RenewalRequest> findPendingRenewalsWithContractEndDateBefore(@Param("status") RenewalStatus status,
                                                                     @Param("date") LocalDate date);
}

