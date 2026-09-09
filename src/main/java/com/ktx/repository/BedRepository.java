package com.ktx.repository;

import java.util.List;
import java.util.Optional;

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

    Optional<Bed> findByIdAndRoomId(Long id, Long roomId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT b FROM Bed b
            JOIN FETCH b.room r
            JOIN FETCH r.building
            WHERE r.building.id = :buildingId
            ORDER BY r.floor DESC, r.roomNumber ASC, b.bedCode ASC
            """)
    List<Bed> findByBuildingId(@org.springframework.data.repository.query.Param("buildingId") Long buildingId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Bed b SET b.status = com.ktx.domain.enums.BedStatus.OCCUPIED, b.currentContractId = :contractId WHERE b.id = :bedId AND b.status = com.ktx.domain.enums.BedStatus.VACANT")
    int occupyBed(@org.springframework.data.repository.query.Param("bedId") Long bedId, @org.springframework.data.repository.query.Param("contractId") Long contractId);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Bed b SET b.status = com.ktx.domain.enums.BedStatus.VACANT, b.currentContractId = null WHERE b.id = :bedId AND b.status = com.ktx.domain.enums.BedStatus.OCCUPIED")
    int vacateBed(@org.springframework.data.repository.query.Param("bedId") Long bedId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bed b WHERE b.id IN :ids ORDER BY b.id ASC")
    List<Bed> findByIdInForUpdate(@org.springframework.data.repository.query.Param("ids") java.util.Collection<Long> ids);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Bed b WHERE b.id = :id")
    java.util.Optional<Bed> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}

