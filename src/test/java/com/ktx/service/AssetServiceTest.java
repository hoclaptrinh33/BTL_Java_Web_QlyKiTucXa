package com.ktx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Room;
import com.ktx.domain.RoomAsset;
import com.ktx.domain.enums.AssetCategory;
import com.ktx.domain.enums.AssetCondition;
import com.ktx.dto.RoomAssetForm;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private RoomAssetRepository roomAssetRepository;
    @Mock
    private RoomRepository roomRepository;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetService(roomAssetRepository, roomRepository);
    }

    @Test
    void create_savesFan() {
        Room room = new Room();
        room.setId(9L);
        when(roomRepository.findByIdWithBuilding(9L)).thenReturn(Optional.of(room));
        when(roomAssetRepository.save(any(RoomAsset.class))).thenAnswer(inv -> inv.getArgument(0));

        RoomAsset saved = assetService.create(9L, form(AssetCategory.FAN, "Quạt trần"));

        assertEquals("Quạt trần", saved.getName());
        assertEquals(AssetCategory.FAN, saved.getCategory());
        verify(roomAssetRepository).save(any(RoomAsset.class));
    }

    @Test
    void create_rejectsSecondElectricMeter() {
        Room room = new Room();
        room.setId(9L);
        RoomAsset existing = new RoomAsset();
        existing.setId(1L);
        existing.setCategory(AssetCategory.ELECTRIC_METER);
        when(roomRepository.findByIdWithBuilding(9L)).thenReturn(Optional.of(room));
        when(roomAssetRepository.findByRoomIdOrderByIdAsc(9L)).thenReturn(List.of(existing));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> assetService.create(9L, form(AssetCategory.ELECTRIC_METER, "Công tơ 2")));
        assertEquals(AssetService.DUPLICATE_METER, ex.getMessage());
        verify(roomAssetRepository, never()).save(any());
    }

    @Test
    void update_allowsSameElectricMeter() {
        RoomAsset existing = new RoomAsset();
        existing.setId(3L);
        existing.setCategory(AssetCategory.ELECTRIC_METER);
        existing.setName("Công tơ cũ");
        when(roomAssetRepository.findByIdAndRoomId(3L, 9L)).thenReturn(Optional.of(existing));
        when(roomAssetRepository.findByRoomIdOrderByIdAsc(9L)).thenReturn(List.of(existing));
        when(roomAssetRepository.save(existing)).thenReturn(existing);

        RoomAssetForm form = form(AssetCategory.ELECTRIC_METER, "Công tơ A");
        form.setSerialNumber("e-11");
        RoomAsset updated = assetService.update(9L, 3L, form);

        assertEquals("Công tơ A", updated.getName());
        assertEquals("E-11", updated.getSerialNumber());
    }

    private static RoomAssetForm form(AssetCategory category, String name) {
        RoomAssetForm form = new RoomAssetForm();
        form.setName(name);
        form.setCategory(category);
        form.setQuantity(1);
        form.setCondition(AssetCondition.GOOD);
        return form;
    }
}
