package com.ktx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ktx.domain.RoomImage;
import com.ktx.domain.enums.RoomType;

@Repository
public interface RoomImageRepository extends JpaRepository<RoomImage, Long> {

    @EntityGraph(attributePaths = {"building"})
    List<RoomImage> findByRoomTypeOrderByDisplayOrderAscIdAsc(RoomType roomType);

    @EntityGraph(attributePaths = {"building"})
    List<RoomImage> findByRoomTypeAndBuildingIdOrderByDisplayOrderAscIdAsc(RoomType roomType, Long buildingId);

    @EntityGraph(attributePaths = {"building"})
    List<RoomImage> findByRoomIdOrderByDisplayOrderAscIdAsc(Long roomId);

    @EntityGraph(attributePaths = {"building"})
    Optional<RoomImage> findFirstByRoomTypeAndPrimaryTrue(RoomType roomType);
}
