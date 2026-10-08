package com.ktx.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.MaintenanceTicket;
import com.ktx.domain.enums.TicketStatus;

public interface MaintenanceTicketRepository extends JpaRepository<MaintenanceTicket, Long> {

    @Query("""
            SELECT t FROM MaintenanceTicket t
            JOIN FETCH t.student s
            JOIN FETCH s.user
            JOIN FETCH t.room r
            JOIN FETCH r.building b
            WHERE t.student.id = :studentId
            ORDER BY t.createdAt DESC
            """)
    List<MaintenanceTicket> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("""
            SELECT t FROM MaintenanceTicket t
            JOIN FETCH t.student s
            JOIN FETCH s.user
            JOIN FETCH t.room r
            JOIN FETCH r.building b
            WHERE r.building.id = :buildingId
            ORDER BY t.createdAt DESC
            """)
    List<MaintenanceTicket> findByBuildingIdWithDetails(@Param("buildingId") Long buildingId);

    @Query("""
            SELECT t FROM MaintenanceTicket t
            JOIN FETCH t.student s
            JOIN FETCH s.user
            JOIN FETCH t.room r
            JOIN FETCH r.building b
            ORDER BY t.createdAt DESC
            """)
    List<MaintenanceTicket> findAllWithDetails();

    @Query(value = """
            SELECT t FROM MaintenanceTicket t
            JOIN FETCH t.student s
            JOIN FETCH s.user
            JOIN FETCH t.room r
            JOIN FETCH r.building b
            WHERE (:buildingId IS NULL OR r.building.id = :buildingId)
              AND (:status IS NULL OR t.status = :status)
            ORDER BY t.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(t) FROM MaintenanceTicket t
            JOIN t.room r
            WHERE (:buildingId IS NULL OR r.building.id = :buildingId)
              AND (:status IS NULL OR t.status = :status)
            """)
    org.springframework.data.domain.Page<MaintenanceTicket> searchTicketsForAdmin(
            @Param("buildingId") Long buildingId,
            @Param("status") TicketStatus status,
            org.springframework.data.domain.Pageable pageable);

    List<MaintenanceTicket> findByStatusAndResolvedAtBefore(TicketStatus status, LocalDateTime cutoff);

    long countByStudentIdAndStatus(Long studentId, TicketStatus status);

    long countByStatus(TicketStatus status);

    long countByStatusAndRoomBuildingId(TicketStatus status, Long buildingId);
}
