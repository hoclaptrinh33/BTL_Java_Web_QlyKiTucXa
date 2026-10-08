package com.ktx.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.RoomChangeRequest;
import com.ktx.domain.enums.RoomChangeKind;
import com.ktx.domain.enums.RoomChangeStatus;

public interface RoomChangeRequestRepository extends JpaRepository<RoomChangeRequest, Long> {

    @Query("""
            SELECT r FROM RoomChangeRequest r
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            JOIN FETCH r.currentBed cb
            JOIN FETCH cb.room cbr
            JOIN FETCH cbr.building cbb
            LEFT JOIN FETCH r.requestedBuilding rb
            LEFT JOIN FETCH r.targetBed tb
            LEFT JOIN FETCH tb.room tbr
            LEFT JOIN FETCH tbr.building tbb
            WHERE r.id = :id
            """)
    Optional<RoomChangeRequest> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT r FROM RoomChangeRequest r
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            JOIN FETCH r.currentBed cb
            JOIN FETCH cb.room cbr
            JOIN FETCH cbr.building cbb
            LEFT JOIN FETCH r.requestedBuilding rb
            LEFT JOIN FETCH r.targetBed tb
            LEFT JOIN FETCH tb.room tbr
            LEFT JOIN FETCH tbr.building tbb
            WHERE s.id = :studentId
            ORDER BY r.id DESC
            """)
    List<RoomChangeRequest> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("""
            SELECT r FROM RoomChangeRequest r
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            JOIN FETCH r.currentBed cb
            JOIN FETCH cb.room cbr
            JOIN FETCH cbr.building cbb
            LEFT JOIN FETCH r.requestedBuilding rb
            LEFT JOIN FETCH r.targetBed tb
            LEFT JOIN FETCH tb.room tbr
            LEFT JOIN FETCH tbr.building tbb
            WHERE s.id = :studentId AND r.requestKind = :kind
            ORDER BY r.id DESC
            """)
    List<RoomChangeRequest> findByStudentIdAndRequestKindWithDetails(@Param("studentId") Long studentId, @Param("kind") RoomChangeKind kind);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM RoomChangeRequest r
            WHERE r.contract.student.id = :studentId
              AND r.requestKind = :kind
              AND r.status IN :statuses
            """)
    boolean existsByStudentIdAndRequestKindAndStatusIn(@Param("studentId") Long studentId,
                                                       @Param("kind") RoomChangeKind kind,
                                                       @Param("statuses") Collection<RoomChangeStatus> statuses);

    @Query("""
            SELECT r FROM RoomChangeRequest r
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            JOIN FETCH r.currentBed cb
            JOIN FETCH cb.room cbr
            JOIN FETCH cbr.building cbb
            LEFT JOIN FETCH r.requestedBuilding rb
            LEFT JOIN FETCH r.targetBed tb
            LEFT JOIN FETCH tb.room tbr
            LEFT JOIN FETCH tbr.building tbb
            WHERE (:kind IS NULL OR r.requestKind = :kind)
              AND (:status IS NULL OR r.status = :status)
              AND (:buildingId IS NULL OR cbb.id = :buildingId OR rb.id = :buildingId)
            ORDER BY r.id DESC
            """)
    List<RoomChangeRequest> searchRequests(@Param("kind") RoomChangeKind kind,
                                          @Param("status") RoomChangeStatus status,
                                          @Param("buildingId") Long buildingId);

    @Query(value = """
            SELECT r FROM RoomChangeRequest r
            JOIN FETCH r.contract c
            JOIN FETCH c.student s
            JOIN FETCH r.currentBed cb
            JOIN FETCH cb.room cbr
            JOIN FETCH cbr.building cbb
            LEFT JOIN FETCH r.requestedBuilding rb
            LEFT JOIN FETCH r.targetBed tb
            LEFT JOIN FETCH tb.room tbr
            LEFT JOIN FETCH tbr.building tbb
            WHERE (:kind IS NULL OR r.requestKind = :kind)
              AND (:status IS NULL OR r.status = :status)
              AND (:buildingId IS NULL OR cbb.id = :buildingId OR rb.id = :buildingId)
            ORDER BY r.id DESC
            """,
           countQuery = """
            SELECT COUNT(r) FROM RoomChangeRequest r
            LEFT JOIN r.currentBed cb
            LEFT JOIN cb.room cbr
            LEFT JOIN cbr.building cbb
            LEFT JOIN r.requestedBuilding rb
            WHERE (:kind IS NULL OR r.requestKind = :kind)
              AND (:status IS NULL OR r.status = :status)
              AND (:buildingId IS NULL OR cbb.id = :buildingId OR rb.id = :buildingId)
            """)
    org.springframework.data.domain.Page<RoomChangeRequest> searchRequests(@Param("kind") RoomChangeKind kind,
                                                                          @Param("status") RoomChangeStatus status,
                                                                          @Param("buildingId") Long buildingId,
                                                                          org.springframework.data.domain.Pageable pageable);
}

