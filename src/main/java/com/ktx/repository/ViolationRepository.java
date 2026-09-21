package com.ktx.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Violation;
import com.ktx.domain.enums.ContractStatus;

public interface ViolationRepository extends JpaRepository<Violation, Long> {

    @Query("""
            SELECT v FROM Violation v
            JOIN FETCH v.student s
            JOIN FETCH s.user
            JOIN FETCH v.recordedBy u
            WHERE v.student.id = :studentId
            ORDER BY v.occurredAt DESC
            """)
    List<Violation> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("""
            SELECT v FROM Violation v
            JOIN FETCH v.student s
            JOIN FETCH s.user
            JOIN FETCH v.recordedBy u
            ORDER BY v.occurredAt DESC
            """)
    List<Violation> findAllWithDetails();

    @Query("""
            SELECT v FROM Violation v
            JOIN FETCH v.student s
            JOIN FETCH s.user
            JOIN FETCH v.recordedBy u
            WHERE (s.id IN (
                SELECT c.student.id FROM Contract c
                WHERE c.bed.room.building.id = :buildingId
                  AND c.status IN :statuses
            ) OR v.recordedBy.id IN (
                SELECT st.user.id FROM Staff st
                WHERE st.assignedBuilding.id = :buildingId
            ))
            ORDER BY v.occurredAt DESC
            """)
    List<Violation> findByBuildingIdWithDetails(@Param("buildingId") Long buildingId,
                                               @Param("statuses") Collection<ContractStatus> statuses);

    long countByStudentId(Long studentId);
}
