package com.ktx.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ktx.domain.RoomAsset;

public interface RoomAssetRepository extends JpaRepository<RoomAsset, Long> {
}
