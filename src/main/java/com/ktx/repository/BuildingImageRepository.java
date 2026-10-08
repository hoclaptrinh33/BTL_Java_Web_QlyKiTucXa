package com.ktx.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ktx.domain.BuildingImage;

@Repository
public interface BuildingImageRepository extends JpaRepository<BuildingImage, Long> {

    List<BuildingImage> findByBuildingIdOrderByDisplayOrderAscIdAsc(Long buildingId);

    Optional<BuildingImage> findFirstByBuildingIdAndPrimaryTrue(Long buildingId);

    void deleteByBuildingId(Long buildingId);
}
