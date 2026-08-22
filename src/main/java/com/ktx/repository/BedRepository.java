package com.ktx.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.ktx.domain.Bed;

public interface BedRepository extends JpaRepository<Bed, Long> {

    @Query("""
            SELECT b FROM Bed b
            JOIN FETCH b.room r
            JOIN FETCH r.building
            """)
    List<Bed> findAllWithRoomAndBuilding();

    long countByRoomIdAndStatus(Long roomId, com.ktx.domain.enums.BedStatus status);

    long countByRoomId(Long roomId);

    List<Bed> findByRoomIdOrderByBedCodeAsc(Long roomId);
}
