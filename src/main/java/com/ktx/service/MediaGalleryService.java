package com.ktx.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.BuildingImage;
import com.ktx.domain.Room;
import com.ktx.domain.RoomImage;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.BuildingGalleryDto;
import com.ktx.dto.RoomTypeGalleryDto;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingImageRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.RoomImageRepository;
import com.ktx.repository.RoomRepository;

@Service
public class MediaGalleryService {

    private final BuildingRepository buildingRepository;
    private final BuildingImageRepository buildingImageRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final RoomImageRepository roomImageRepository;
    private final FileStorageService fileStorageService;

    public MediaGalleryService(BuildingRepository buildingRepository,
                               BuildingImageRepository buildingImageRepository,
                               RoomRepository roomRepository,
                               BedRepository bedRepository,
                               RoomImageRepository roomImageRepository,
                               FileStorageService fileStorageService) {
        this.buildingRepository = buildingRepository;
        this.buildingImageRepository = buildingImageRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.roomImageRepository = roomImageRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional(readOnly = true)
    public List<BuildingGalleryDto> getBuildingGalleries(BuildingGenderPolicy genderPolicy) {
        List<Building> buildings = buildingRepository.findAll();
        List<BuildingGalleryDto> result = new ArrayList<>();

        for (Building b : buildings) {
            if (!Boolean.TRUE.equals(b.getActive())) {
                continue;
            }
            if (genderPolicy != null && b.getGenderPolicy() != genderPolicy) {
                continue;
            }

            BuildingGalleryDto dto = new BuildingGalleryDto();
            dto.setId(b.getId());
            dto.setCode(b.getCode());
            dto.setName(b.getName());
            dto.setGenderPolicy(b.getGenderPolicy());
            dto.setGenderLabel(b.getGenderPolicy() == BuildingGenderPolicy.MALE ? "Nam" : "Nữ");
            dto.setAddress("Khuôn viên Ký túc xá — Khu " + b.getCode());

            List<BuildingImage> images = buildingImageRepository.findByBuildingIdOrderByDisplayOrderAscIdAsc(b.getId());
            dto.setTotalImages(images.size());

            String primaryUrl = null;
            for (BuildingImage img : images) {
                dto.getImageUrls().add(img.getImageUrl());
                dto.getCaptions().add(img.getCaption() != null ? img.getCaption() : b.getName());
                if (Boolean.TRUE.equals(img.getPrimary()) && primaryUrl == null) {
                    primaryUrl = img.getImageUrl();
                }
            }

            if (primaryUrl == null && !images.isEmpty()) {
                primaryUrl = images.get(0).getImageUrl();
            }
            if (primaryUrl == null) {
                primaryUrl = (b.getGenderPolicy() == BuildingGenderPolicy.FEMALE)
                        ? "/images/buildings/building-female-1.jpg"
                        : "/images/buildings/building-male-1.jpg";
                dto.getImageUrls().add(primaryUrl);
                dto.getCaptions().add("Ảnh tòa " + b.getName());
            }
            dto.setPrimaryImageUrl(primaryUrl);

            // Metrics
            List<Room> rooms = roomRepository.findByBuildingIdWithBuilding(b.getId());
            dto.setRoomCount(rooms.size());

            List<Bed> beds = bedRepository.findByBuildingId(b.getId());
            dto.setBedCount(beds.size());
            int vacant = 0;
            for (Bed bed : beds) {
                if (bed.getStatus() == BedStatus.VACANT) {
                    vacant++;
                }
            }
            // Rich description and amenities
            if ("A".equalsIgnoreCase(b.getCode())) {
                dto.setDescription("Tòa 5 tầng gần sân thể thao, nhà xe ngầm, khuôn viên cây xanh thoáng mát.");
                dto.setAmenities(List.of("Nhà xe tầng hầm", "Sân bóng đá & TDTT", "Bảo vệ trực 24/7", "Camera an ninh", "Máy giặt chung", "Wifi cáp quang"));
            } else if ("B".equalsIgnoreCase(b.getCode())) {
                dto.setDescription("Tòa 5 tầng dành riêng cho Nữ, khuôn viên yên tĩnh, an ninh và kiểm soát vân tay nghiêm ngặt.");
                dto.setAmenities(List.of("Cổng quét vân tay", "Bảo vệ trực 24/7", "Khu tự học tầng 1", "Nhà xe có mái che", "Máy sấy quần áo", "Wifi tốc độ cao"));
            } else if ("C".equalsIgnoreCase(b.getCode())) {
                dto.setDescription("Tòa Nam sinh viên mới nâng cấp, đầy đủ tiện ích sinh hoạt và không gian học tập.");
                dto.setAmenities(List.of("Sân bóng rổ", "Nhà xe camera giám sát", "Phòng sinh hoạt chung", "Bảo vệ trực 24/7", "Wifi cáp quang tốc độ cao"));
            } else {
                dto.setDescription("Khuôn viên khang trang, an ninh trật tự 24/7, đầy đủ tiện ích sinh hoạt.");
                dto.setAmenities(List.of("Bảo vệ 24/7", "Nhà để xe KTX", "Wifi sinh viên", "Camera an ninh"));
            }

            result.add(dto);
        }

        result.sort((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()));
        return result;
    }

    @Transactional(readOnly = true)
    public List<RoomTypeGalleryDto> getRoomTypeGalleries() {
        List<RoomTypeGalleryDto> list = new ArrayList<>();

        Map<RoomType, RoomTypeMetadata> metaMap = new LinkedHashMap<>();
        metaMap.put(RoomType.STANDARD_4, new RoomTypeMetadata(
                "Phòng 4 giường tiêu chuẩn",
                "Không gian tiện nghi, thông thoáng, tối ưu cho học tập",
                "2.400.000 đ/kỳ",
                4,
                List.of("Điều hòa 2 chiều", "Bình nóng lạnh", "Bàn học & giá sách riêng", "Tủ đồ cá nhân có khóa", "Ban công phơi đồ", "Wifi tốc độ cao"),
                "Mỗi phòng gồm 2 giường tầng cao cấp, bàn học rộng rãi, đầy đủ ánh sáng tự nhiên và khu vệ sinh khép kín sạch sẽ."
        ));

        metaMap.put(RoomType.STANDARD_6, new RoomTypeMetadata(
                "Phòng 6 giường tiêu chuẩn",
                "Chi phí hợp lý, năng động, gắn kết sinh viên",
                "1.800.000 đ/kỳ",
                6,
                List.of("Quạt trần & thông gió", "Bình nóng lạnh", "Bàn học cá nhân", "Tủ sắt 6 ngăn khóa riêng", "Ban công phơi đồ", "Wifi tốc độ cao"),
                "Phòng 6 người được thiết kế tối ưu hóa diện tích sinh hoạt chung, thích hợp cho sinh viên muốn tiết kiệm chi phí nhưng vẫn đảm bảo tiện nghi."
        ));

        metaMap.put(RoomType.STANDARD_8, new RoomTypeMetadata(
                "Phòng 8 giường tiết kiệm",
                "Tiết kiệm tối đa chi phí lưu trú cho sinh viên",
                "1.200.000 đ/kỳ",
                8,
                List.of("Quạt trần công suất lớn", "Bình nóng lạnh", "Tủ đồ cá nhân", "Khu vệ sinh riêng", "Wifi sinh viên"),
                "Lựa chọn tiết kiệm nhất trong ký túc xá, không khí tập thể vui vẻ, an ninh đảm bảo 24/7."
        ));

        metaMap.put(RoomType.VIP_AC, new RoomTypeMetadata(
                "Phòng VIP Cao cấp",
                "Trải nghiệm cao cấp, đầy đủ tiện nghi khép kín",
                "4.000.000 đ/kỳ",
                2,
                List.of("Điều hòa Inverter", "Bình nóng lạnh", "Tủ lạnh mini", "Bàn học & ghế xoay", "Tủ quần áo gỗ", "Phòng tắm kính cao cấp", "Wifi riêng từng phòng"),
                "Không gian riêng tư, trang thiết bị nội thất cao cấp tương đương căn hộ dịch vụ mini, phù hợp sinh viên cần không gian yên tĩnh tuyệt đối."
        ));

        for (Map.Entry<RoomType, RoomTypeMetadata> entry : metaMap.entrySet()) {
            RoomType type = entry.getKey();
            RoomTypeMetadata meta = entry.getValue();

            RoomTypeGalleryDto dto = new RoomTypeGalleryDto();
            dto.setType(type);
            dto.setTypeCode(type.name());
            dto.setTitle(meta.title);
            dto.setSubtitle(meta.subtitle);
            dto.setPriceFormatted(meta.priceFormatted);
            dto.setCapacity(meta.capacity);
            dto.setAmenities(meta.amenities);
            dto.setDescription(meta.description);

            List<RoomImage> images = roomImageRepository.findByRoomTypeOrderByDisplayOrderAscIdAsc(type);
            String primaryUrl = null;
            for (RoomImage img : images) {
                dto.getImageUrls().add(img.getImageUrl());
                dto.getCaptions().add(img.getCaption() != null ? img.getCaption() : meta.title);
                if (Boolean.TRUE.equals(img.getPrimary()) && primaryUrl == null) {
                    primaryUrl = img.getImageUrl();
                }
            }

            if (primaryUrl == null && !images.isEmpty()) {
                primaryUrl = images.get(0).getImageUrl();
            }
            if (primaryUrl == null) {
                primaryUrl = switch (type) {
                    case VIP_AC -> "/images/rooms/room-vip-ac.jpg";
                    case STANDARD_8 -> "/images/rooms/room-standard-8.jpg";
                    case STANDARD_6 -> "/images/rooms/room-standard-6.jpg";
                    default -> "/images/rooms/room-standard-4.jpg";
                };
                dto.getImageUrls().add(primaryUrl);
                dto.getCaptions().add("Ảnh minh họa " + meta.title);
            }
            dto.setPrimaryImageUrl(primaryUrl);

            list.add(dto);
        }

        return list;
    }

    @Transactional
    public BuildingImage addBuildingImage(Long buildingId, MultipartFile file, String caption, boolean isPrimary) {
        Building building = buildingRepository.findById(buildingId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tòa nhà ID: " + buildingId));

        String url = fileStorageService.storeFile(file, "buildings");

        if (isPrimary) {
            clearBuildingPrimary(buildingId);
        }

        BuildingImage image = new BuildingImage();
        image.setBuilding(building);
        image.setImageUrl(url);
        image.setCaption(caption);
        image.setPrimary(isPrimary);

        List<BuildingImage> existing = buildingImageRepository.findByBuildingIdOrderByDisplayOrderAscIdAsc(buildingId);
        image.setDisplayOrder(existing.size() + 1);

        if (existing.isEmpty()) {
            image.setPrimary(true);
        }

        return buildingImageRepository.save(image);
    }

    @Transactional
    public void deleteBuildingImage(Long imageId) {
        Optional<BuildingImage> opt = buildingImageRepository.findById(imageId);
        if (opt.isPresent()) {
            BuildingImage img = opt.get();
            Long bldgId = img.getBuilding().getId();
            boolean wasPrimary = Boolean.TRUE.equals(img.getPrimary());

            fileStorageService.deleteFile(img.getImageUrl());
            buildingImageRepository.delete(img);

            if (wasPrimary) {
                List<BuildingImage> remaining = buildingImageRepository.findByBuildingIdOrderByDisplayOrderAscIdAsc(bldgId);
                if (!remaining.isEmpty()) {
                    BuildingImage first = remaining.get(0);
                    first.setPrimary(true);
                    buildingImageRepository.save(first);
                }
            }
        }
    }

    @Transactional
    public void setBuildingPrimaryImage(Long buildingId, Long imageId) {
        clearBuildingPrimary(buildingId);
        BuildingImage img = buildingImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh ID: " + imageId));
        img.setPrimary(true);
        buildingImageRepository.save(img);
    }

    private void clearBuildingPrimary(Long buildingId) {
        List<BuildingImage> images = buildingImageRepository.findByBuildingIdOrderByDisplayOrderAscIdAsc(buildingId);
        for (BuildingImage img : images) {
            if (Boolean.TRUE.equals(img.getPrimary())) {
                img.setPrimary(false);
                buildingImageRepository.save(img);
            }
        }
    }

    @Transactional
    public RoomImage addRoomTypeImage(RoomType roomType, Long buildingId, MultipartFile file, String caption, boolean isPrimary) {
        Building building = null;
        if (buildingId != null) {
            building = buildingRepository.findById(buildingId).orElse(null);
        }

        String url = fileStorageService.storeFile(file, "rooms");

        if (isPrimary) {
            clearRoomTypePrimary(roomType);
        }

        RoomImage image = new RoomImage();
        image.setRoomType(roomType);
        image.setBuilding(building);
        image.setImageUrl(url);
        image.setCaption(caption);
        image.setPrimary(isPrimary);

        List<RoomImage> existing = roomImageRepository.findByRoomTypeOrderByDisplayOrderAscIdAsc(roomType);
        image.setDisplayOrder(existing.size() + 1);

        if (existing.isEmpty()) {
            image.setPrimary(true);
        }

        return roomImageRepository.save(image);
    }

    @Transactional
    public void deleteRoomImage(Long imageId) {
        Optional<RoomImage> opt = roomImageRepository.findById(imageId);
        if (opt.isPresent()) {
            RoomImage img = opt.get();
            RoomType type = img.getRoomType();
            boolean wasPrimary = Boolean.TRUE.equals(img.getPrimary());

            fileStorageService.deleteFile(img.getImageUrl());
            roomImageRepository.delete(img);

            if (wasPrimary) {
                List<RoomImage> remaining = roomImageRepository.findByRoomTypeOrderByDisplayOrderAscIdAsc(type);
                if (!remaining.isEmpty()) {
                    RoomImage first = remaining.get(0);
                    first.setPrimary(true);
                    roomImageRepository.save(first);
                }
            }
        }
    }

    @Transactional
    public void setRoomTypePrimaryImage(RoomType roomType, Long imageId) {
        clearRoomTypePrimary(roomType);
        RoomImage img = roomImageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy ảnh ID: " + imageId));
        img.setPrimary(true);
        roomImageRepository.save(img);
    }

    private void clearRoomTypePrimary(RoomType roomType) {
        List<RoomImage> images = roomImageRepository.findByRoomTypeOrderByDisplayOrderAscIdAsc(roomType);
        for (RoomImage img : images) {
            if (Boolean.TRUE.equals(img.getPrimary())) {
                img.setPrimary(false);
                roomImageRepository.save(img);
            }
        }
    }

    private static class RoomTypeMetadata {
        final String title;
        final String subtitle;
        final String priceFormatted;
        final int capacity;
        final List<String> amenities;
        final String description;

        RoomTypeMetadata(String title, String subtitle, String priceFormatted, int capacity, List<String> amenities, String description) {
            this.title = title;
            this.subtitle = subtitle;
            this.priceFormatted = priceFormatted;
            this.capacity = capacity;
            this.amenities = amenities;
            this.description = description;
        }
    }
}
