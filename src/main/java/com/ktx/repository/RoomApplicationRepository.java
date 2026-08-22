package com.ktx.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ktx.domain.RoomApplication;

public interface RoomApplicationRepository extends JpaRepository<RoomApplication, Long> {

    @Query("""
            SELECT a FROM RoomApplication a
            JOIN FETCH a.student
            LEFT JOIN FETCH a.preferredBuilding
            ORDER BY a.submittedAt DESC
            """)
    List<RoomApplication> findRecent(Pageable pageable);
}
