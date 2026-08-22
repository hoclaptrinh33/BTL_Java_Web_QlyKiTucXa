package com.ktx.service;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Room;
import com.ktx.domain.RoomAsset;
import com.ktx.domain.enums.AssetCategory;
import com.ktx.domain.enums.AssetCondition;
import com.ktx.dto.RoomAssetForm;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.RoomRepository;

@Service
public class AssetService {

    public static final String NOT_FOUND = "Không tìm thấy tài sản";
    public static final String ROOM_NOT_FOUND = RoomService.NOT_FOUND;
    public static final String DUPLICATE_METER = "Mỗi phòng chỉ một công tơ điện và một công tơ nước";

    private final RoomAssetRepository roomAssetRepository;
    private final RoomRepository roomRepository;

    public AssetService(RoomAssetRepository roomAssetRepository, RoomRepository roomRepository) {
        this.roomAssetRepository = roomAssetRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional(readOnly = true)
    public List<RoomAsset> list(Long roomId) {
        return roomAssetRepository.findByRoomIdOrderByIdAsc(roomId);
    }

    @Transactional(readOnly = true)
    public RoomAsset getById(Long roomId, Long assetId) {
        return roomAssetRepository.findByIdAndRoomId(assetId, roomId)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    @Transactional
    public RoomAsset create(Long roomId, RoomAssetForm form) {
        Room room = roomRepository.findByIdWithBuilding(roomId)
                .orElseThrow(() -> new BusinessException(ROOM_NOT_FOUND));
        assertUniqueMeter(roomId, form.getCategory(), null);
        RoomAsset asset = new RoomAsset();
        asset.setRoom(room);
        apply(asset, form);
        return roomAssetRepository.save(asset);
    }

    @Transactional
    public RoomAsset update(Long roomId, Long assetId, RoomAssetForm form) {
        RoomAsset asset = getById(roomId, assetId);
        assertUniqueMeter(roomId, form.getCategory(), asset.getId());
        apply(asset, form);
        return roomAssetRepository.save(asset);
    }

    @Transactional
    public void delete(Long roomId, Long assetId) {
        RoomAsset asset = getById(roomId, assetId);
        roomAssetRepository.delete(asset);
    }

    public static String categoryLabel(AssetCategory category) {
        if (category == null) {
            return "—";
        }
        return switch (category) {
            case FAN -> "Quạt";
            case BED_FRAME -> "Giường khung";
            case CABINET -> "Tủ";
            case DESK -> "Bàn";
            case ELECTRIC_METER -> "Công tơ điện";
            case WATER_METER -> "Công tơ nước";
            case AC -> "Điều hòa";
            case OTHER -> "Khác";
        };
    }

    public static String conditionLabel(AssetCondition condition) {
        if (condition == null) {
            return "—";
        }
        return switch (condition) {
            case GOOD -> "Tốt";
            case DAMAGED -> "Hỏng";
            case MAINTENANCE -> "Bảo trì";
        };
    }

    private void assertUniqueMeter(Long roomId, AssetCategory category, Long excludeId) {
        if (category != AssetCategory.ELECTRIC_METER && category != AssetCategory.WATER_METER) {
            return;
        }
        for (RoomAsset existing : roomAssetRepository.findByRoomIdOrderByIdAsc(roomId)) {
            if (existing.getCategory() == category && (excludeId == null || !excludeId.equals(existing.getId()))) {
                throw new BusinessException(DUPLICATE_METER);
            }
        }
    }

    private static void apply(RoomAsset asset, RoomAssetForm form) {
        asset.setName(form.getName() == null ? "" : form.getName().trim());
        asset.setCategory(form.getCategory());
        asset.setQuantity(form.getQuantity() == null ? 1 : form.getQuantity());
        asset.setCondition(form.getCondition() == null ? AssetCondition.GOOD : form.getCondition());
        asset.setNote(StringUtils.hasText(form.getNote()) ? form.getNote().trim() : null);
        String serial = form.getSerialNumber() == null ? null : form.getSerialNumber().trim().toUpperCase(Locale.ROOT);
        asset.setSerialNumber(StringUtils.hasText(serial) ? serial : null);
    }
}
