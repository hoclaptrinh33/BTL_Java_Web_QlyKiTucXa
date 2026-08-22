package com.ktx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ktx.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("SELECT r FROM Room r JOIN FETCH r.building ORDER BY r.building.code, r.floor, r.roomNumber")
    List<Room> findAllWithBuilding();

    @Query("SELECT r FROM Room r JOIN FETCH r.building WHERE r.building.id = :buildingId ORDER BY r.floor, r.roomNumber")
    List<Room> findByBuildingIdWithBuilding(@Param("buildingId") Long buildingId);

    @Query("SELECT r FROM Room r JOIN FETCH r.building WHERE r.id = :id")
    Optional<Room> findByIdWithBuilding(@Param("id") Long id);

    boolean existsByBuildingId(Long buildingId);

    boolean existsByBuildingIdAndRoomNumber(Long buildingId, String roomNumber);

    boolean existsByBuildingIdAndRoomNumberAndIdNot(Long buildingId, String roomNumber, Long id);
}
