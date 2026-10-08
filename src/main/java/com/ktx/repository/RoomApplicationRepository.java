package com.ktx.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;

import com.ktx.domain.RoomApplication;

public interface RoomApplicationRepository extends JpaRepository<RoomApplication, Long> {

    @Query("""
            SELECT a FROM RoomApplication a
            JOIN FETCH a.student
            LEFT JOIN FETCH a.preferredBuilding
            ORDER BY a.submittedAt DESC
            """)
    List<RoomApplication> findRecent(Pageable pageable);

    boolean existsByPeriodId(Long periodId);

    @Query("""
            SELECT a FROM RoomApplication a
            JOIN FETCH a.period p
            LEFT JOIN FETCH a.preferredBuilding b
            WHERE a.student.id = :studentId
            ORDER BY p.openAt DESC
            """)
    List<RoomApplication> findByStudentIdOrderByPeriodOpenAtDesc(@Param("studentId") Long studentId);

    boolean existsByPeriodIdAndStudentId(Long periodId, Long studentId);

    java.util.Optional<RoomApplication> findByPeriodIdAndStudentId(Long periodId, Long studentId);

    @Query("""
            SELECT a FROM RoomApplication a
            JOIN FETCH a.student s
            LEFT JOIN FETCH a.preferredBuilding b
            WHERE a.period.id = :periodId
            ORDER BY a.submittedAt DESC
            """)
    List<RoomApplication> findByPeriodIdWithDetails(@Param("periodId") Long periodId);

    @Query(value = """
            SELECT a FROM RoomApplication a
            JOIN FETCH a.student s
            LEFT JOIN FETCH a.preferredBuilding b
            WHERE a.period.id = :periodId
            ORDER BY a.id DESC
            """,
            countQuery = "SELECT COUNT(a) FROM RoomApplication a WHERE a.period.id = :periodId")
    org.springframework.data.domain.Page<RoomApplication> findPageByPeriodId(@Param("periodId") Long periodId,
                                                                            org.springframework.data.domain.Pageable pageable);

    @Query("""
            SELECT a FROM RoomApplication a
            JOIN FETCH a.student s
            LEFT JOIN FETCH a.preferredBuilding b
            WHERE a.period.id = :periodId
              AND a.status = :status
            """)
    List<RoomApplication> findByPeriodIdAndStatus(@Param("periodId") Long periodId,
            @Param("status") com.ktx.domain.enums.ApplicationStatus status);

    long countByPeriodIdAndStatus(Long periodId, com.ktx.domain.enums.ApplicationStatus status);
}
