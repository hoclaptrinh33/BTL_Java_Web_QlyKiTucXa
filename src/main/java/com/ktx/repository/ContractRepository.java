package com.ktx.repository;

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
}

