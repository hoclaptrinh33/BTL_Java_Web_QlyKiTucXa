package com.ktx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.RoomAsset;

public interface RoomAssetRepository extends JpaRepository<RoomAsset, Long> {

    List<RoomAsset> findByRoomIdOrderByIdAsc(Long roomId);

    Optional<RoomAsset> findByIdAndRoomId(Long id, Long roomId);
}
