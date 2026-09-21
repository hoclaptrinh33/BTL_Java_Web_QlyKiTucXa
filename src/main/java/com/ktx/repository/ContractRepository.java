package com.ktx.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Contract;
import com.ktx.domain.enums.ContractStatus;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Contract c
            WHERE c.bed.room.building.id = :buildingId
              AND c.status IN :statuses
            """)
    boolean existsOccupyingInBuilding(@Param("buildingId") Long buildingId,
            @Param("statuses") Collection<ContractStatus> statuses);

    long countByStatusIn(Collection<ContractStatus> statuses);

    @Query("""
            SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
            FROM Contract c
            WHERE c.student.id = :studentId
              AND c.status IN :statuses
            """)
    boolean existsByStudentIdAndStatusIn(@Param("studentId") Long studentId,
            @Param("statuses") Collection<ContractStatus> statuses);

    @Query("SELECT c.student.id FROM Contract c WHERE c.status IN :statuses")
    List<Long> findStudentIdsByStatusIn(@Param("statuses") Collection<ContractStatus> statuses);

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.bed b
            JOIN FETCH b.room r
            JOIN FETCH r.building build
            JOIN FETCH c.student s
            WHERE c.status IN :statuses
            """)
    List<Contract> findOccupyingWithDetails(@Param("statuses") Collection<ContractStatus> statuses);

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.student
            WHERE c.bed.room.building.id = :buildingId
              AND c.status IN :statuses
            """)
    List<Contract> findOccupyingContractsByBuildingId(
            @Param("buildingId") Long buildingId,
            @Param("statuses") Collection<ContractStatus> statuses);

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.bed b
            JOIN FETCH b.room r
            JOIN FETCH r.building build
            WHERE c.student.id = :studentId
              AND c.status IN :statuses
            """)
    List<Contract> findByStudentIdAndStatusInWithDetails(
            @Param("studentId") Long studentId,
            @Param("statuses") Collection<ContractStatus> statuses);

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.student s
            JOIN FETCH c.bed b
            WHERE c.bed.room.id = :roomId
              AND c.status IN :statuses
            """)
    List<Contract> findOccupyingByRoomId(
            @Param("roomId") Long roomId,
            @Param("statuses") Collection<ContractStatus> statuses);

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.student s
            JOIN FETCH s.user u
            WHERE c.status IN :statuses
              AND c.endDate = :targetDate
            """)
    List<Contract> findExpiringContracts(
            @Param("statuses") Collection<ContractStatus> statuses,
            @Param("targetDate") LocalDate targetDate);

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.student s
            JOIN FETCH c.bed b
            JOIN FETCH b.room r
            JOIN FETCH r.building build
            LEFT JOIN FETCH c.application app
            WHERE c.id = :id
            """)
    java.util.Optional<Contract> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.student s
            JOIN FETCH c.bed b
            JOIN FETCH b.room r
            JOIN FETCH r.building build
            WHERE (:buildingId IS NULL OR r.building.id = :buildingId)
              AND (:status IS NULL OR c.status = :status)
              AND (:keyword IS NULL OR :keyword = ''
                   OR LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(c.contractNo) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY c.id DESC
            """)
    List<Contract> searchContracts(@Param("buildingId") Long buildingId,
                                   @Param("status") ContractStatus status,
                                   @Param("keyword") String keyword);

    @Query("""
            SELECT c FROM Contract c
            JOIN FETCH c.student s
            JOIN FETCH c.bed b
            JOIN FETCH b.room r
            JOIN FETCH r.building build
            WHERE r.building.id = :buildingId
              AND c.status IN :statuses
            ORDER BY r.floor DESC, r.roomNumber ASC, b.bedCode ASC
            """)
    List<Contract> findByBuildingIdAndStatusInWithDetails(
            @Param("buildingId") Long buildingId,
            @Param("statuses") Collection<ContractStatus> statuses);
}

