package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.ktx.domain.Building;
import com.ktx.domain.BuildingImage;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.BuildingGalleryDto;
import com.ktx.dto.RoomTypeGalleryDto;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingImageRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.RoomImageRepository;
import com.ktx.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class MediaGalleryServiceTest {

    @Mock
    private BuildingRepository buildingRepository;
    @Mock
    private BuildingImageRepository buildingImageRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private BedRepository bedRepository;
    @Mock
    private RoomImageRepository roomImageRepository;
    @Mock
    private FileStorageService fileStorageService;

    private MediaGalleryService mediaGalleryService;

    @BeforeEach
    void setUp() {
        mediaGalleryService = new MediaGalleryService(
                buildingRepository,
                buildingImageRepository,
                roomRepository,
                bedRepository,
                roomImageRepository,
                fileStorageService
        );
    }

    @Test
    void getBuildingGalleries_returnsPopulatedList() {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");
        b.setName("Tòa A");
        b.setActive(true);
        b.setGenderPolicy(BuildingGenderPolicy.MALE);

        when(buildingRepository.findAll()).thenReturn(List.of(b));
        BuildingImage img = new BuildingImage(b, "/uploads/buildings/test.jpg", "Mặt tiền", true, 1);
        when(buildingImageRepository.findByBuildingIdOrderByDisplayOrderAscIdAsc(1L)).thenReturn(List.of(img));
        when(roomRepository.findByBuildingIdWithBuilding(1L)).thenReturn(List.of());
        when(bedRepository.findByBuildingId(1L)).thenReturn(List.of());

        List<BuildingGalleryDto> result = mediaGalleryService.getBuildingGalleries(null);
        assertEquals(1, result.size());
        assertEquals("Tòa A", result.get(0).getName());
        assertEquals("/uploads/buildings/test.jpg", result.get(0).getPrimaryImageUrl());
        assertEquals(1, result.get(0).getTotalImages());
    }

    @Test
    void getRoomTypeGalleries_returnsAllFourRoomTypes() {
        when(roomImageRepository.findByRoomTypeOrderByDisplayOrderAscIdAsc(any())).thenReturn(List.of());

        List<RoomTypeGalleryDto> list = mediaGalleryService.getRoomTypeGalleries();
        assertEquals(4, list.size());
        assertTrue(list.stream().anyMatch(dto -> dto.getType() == RoomType.STANDARD_4));
        assertTrue(list.stream().anyMatch(dto -> dto.getType() == RoomType.VIP_AC));
    }

    @Test
    void addBuildingImage_savesCorrectly() {
        Building b = new Building();
        b.setId(1L);
        b.setCode("A");

        when(buildingRepository.findById(1L)).thenReturn(Optional.of(b));
        when(fileStorageService.storeFile(any(), eq("buildings"))).thenReturn("/uploads/buildings/saved.jpg");
        when(buildingImageRepository.findByBuildingIdOrderByDisplayOrderAscIdAsc(1L)).thenReturn(List.of());
        when(buildingImageRepository.save(any(BuildingImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("files", "test.jpg", "image/jpeg", "content".getBytes());
        BuildingImage saved = mediaGalleryService.addBuildingImage(1L, file, "Cổng chính", true);

        assertNotNull(saved);
        assertEquals("/uploads/buildings/saved.jpg", saved.getImageUrl());
        assertTrue(saved.getPrimary());
        verify(buildingImageRepository).save(any(BuildingImage.class));
    }

    @Test
    void deleteBuildingImage_deletesFileAndRecord() {
        Building b = new Building();
        b.setId(1L);
        BuildingImage img = new BuildingImage(b, "/uploads/buildings/del.jpg", "Xóa", false, 1);
        img.setId(10L);

        when(buildingImageRepository.findById(10L)).thenReturn(Optional.of(img));

        mediaGalleryService.deleteBuildingImage(10L);

        verify(fileStorageService).deleteFile("/uploads/buildings/del.jpg");
        verify(buildingImageRepository).delete(img);
    }
}
