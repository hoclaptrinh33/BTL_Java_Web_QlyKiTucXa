package com.ktx.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Room;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.dto.BuildingForm;
import com.ktx.dto.BuildingOverviewDto;
import com.ktx.dto.BuildingRowDto;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.RoomRepository;

@Service
public class BuildingService {

    public static final String DUPLICATE_CODE = "Mã tòa đã được sử dụng";
    public static final String GENDER_LOCKED =
            "Không đổi giới tính tòa khi còn hợp đồng đang giữ giường";
    public static final String NOT_FOUND = "Không tìm thấy tòa";
    public static final String CANNOT_DELETE_HAS_ROOMS = "Không thể xóa tòa đang có phòng";
    public static final String CANNOT_DELETE_OCCUPIED = "Không thể xóa tòa đang có hợp đồng hoạt động";

    private final BuildingRepository buildingRepository;
    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;

    @Autowired
    public BuildingService(BuildingRepository buildingRepository, ContractRepository contractRepository,
            RoomRepository roomRepository, BedRepository bedRepository) {
        this.buildingRepository = buildingRepository;
        this.contractRepository = contractRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
    }

    public BuildingService(BuildingRepository buildingRepository, ContractRepository contractRepository) {
        this(buildingRepository, contractRepository, null, null);
    }

    @Transactional(readOnly = true)
    public List<Building> listAll() {
        return buildingRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Building getById(Long id) {
        return buildingRepository.findById(id)
                .orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public boolean hasOccupyingContracts(Long buildingId) {
        return contractRepository.existsOccupyingInBuilding(buildingId, OccupyingStatuses.OCCUPYING);
    }

    @Transactional
    public Building create(BuildingForm form) {
        String code = normalizeCode(form.getCode());
        if (buildingRepository.existsByCode(code)) {
            throw new DuplicateFieldException("code", DUPLICATE_CODE);
        }
        Building building = new Building();
        apply(building, form, code);
        return buildingRepository.save(building);
    }

    @Transactional
    public Building update(Long id, BuildingForm form) {
        Building building = getById(id);
        String code = normalizeCode(form.getCode());
        if (buildingRepository.existsByCodeAndIdNot(code, id)) {
            throw new DuplicateFieldException("code", DUPLICATE_CODE);
        }
        BuildingGenderPolicy nextGender = form.getGenderPolicy();
        if (building.getGenderPolicy() != nextGender && hasOccupyingContracts(id)) {
            throw new BusinessException(GENDER_LOCKED);
        }
        apply(building, form, code);
        return buildingRepository.save(building);
    }

    @Transactional
    public void delete(Long id) {
        Building building = getById(id);
        if (hasOccupyingContracts(id)) {
            throw new BusinessException(CANNOT_DELETE_OCCUPIED);
        }
        if (roomRepository != null && roomRepository.existsByBuildingId(id)) {
            throw new BusinessException(CANNOT_DELETE_HAS_ROOMS);
        }
        buildingRepository.delete(building);
    }

    @Transactional(readOnly = true)
    public BuildingOverviewDto loadOverviewStats() {
        BuildingOverviewDto overview = new BuildingOverviewDto();
        List<Building> allBuildings = buildingRepository.findAll();
        overview.setTotalBuildings(allBuildings.size());

        if (roomRepository == null || bedRepository == null) {
            return overview;
        }

        List<Room> allRooms = roomRepository.findAllWithBuilding();
        List<Bed> allBeds = bedRepository.findAllWithRoomAndBuilding();

        long totalRooms = allRooms.stream().filter(r -> r.getStatus() != RoomStatus.INACTIVE).count();
        overview.setTotalRooms(totalRooms);

        long totalBeds = 0;
        long occupied = 0;
        long vacant = 0;
        long maintenance = 0;

        for (Bed bed : allBeds) {
            Room room = bed.getRoom();
            if (room.getStatus() == RoomStatus.INACTIVE) {
                continue;
            }
            totalBeds++;
            if (bed.getStatus() == BedStatus.OCCUPIED) {
                occupied++;
            } else if (bed.getStatus() == BedStatus.VACANT) {
                vacant++;
            } else {
                maintenance++;
            }
        }

        overview.setTotalBeds(totalBeds);
        overview.setOccupiedBeds(occupied);
        overview.setVacantBeds(vacant);
        overview.setMaintenanceBeds(maintenance);
        overview.setOccupancyPercent(percent(occupied, totalBeds));
        overview.setVacantPercent(percent(vacant, totalBeds));

        return overview;
    }

    @Transactional(readOnly = true)
    public List<BuildingRowDto> listBuildingRows(String search, String gender, Boolean active) {
        List<Building> allBuildings = buildingRepository.findAll();
        List<Room> allRooms = (roomRepository != null) ? roomRepository.findAllWithBuilding() : List.of();
        List<Bed> allBeds = (bedRepository != null) ? bedRepository.findAllWithRoomAndBuilding() : List.of();

        // Group rooms by building
        Map<Long, List<Room>> roomsByBuilding = new HashMap<>();
        for (Room room : allRooms) {
            roomsByBuilding.computeIfAbsent(room.getBuilding().getId(), k -> new ArrayList<>()).add(room);
        }

        // Group beds by building
        Map<Long, List<Bed>> bedsByBuilding = new HashMap<>();
        for (Bed bed : allBeds) {
            Room room = bed.getRoom();
            if (room != null && room.getBuilding() != null) {
                bedsByBuilding.computeIfAbsent(room.getBuilding().getId(), k -> new ArrayList<>()).add(bed);
            }
        }

        List<BuildingRowDto> rows = new ArrayList<>();
        String[] fallbackImages = {
            "/images/buildings/building-1.jpg",
            "/images/buildings/building-2.jpg",
            "/images/buildings/building-3.jpg"
        };

        for (int i = 0; i < allBuildings.size(); i++) {
            Building b = allBuildings.get(i);

            // Filter logic
            if (search != null && !search.isBlank()) {
                String q = search.trim().toLowerCase(Locale.ROOT);
                boolean matchesCode = b.getCode() != null && b.getCode().toLowerCase(Locale.ROOT).contains(q);
                boolean matchesName = b.getName() != null && b.getName().toLowerCase(Locale.ROOT).contains(q);
                if (!matchesCode && !matchesName) {
                    continue;
                }
            }

            if (gender != null && !gender.isBlank() && !"ALL".equalsIgnoreCase(gender)) {
                if (!b.getGenderPolicy().name().equalsIgnoreCase(gender)) {
                    continue;
                }
            }

            if (active != null) {
                if (!Boolean.valueOf(b.getActive()).equals(active)) {
                    continue;
                }
            }

            BuildingRowDto row = new BuildingRowDto();
            row.setId(b.getId());
            row.setCode(b.getCode());
            row.setName(b.getName());
            row.setGenderPolicy(b.getGenderPolicy());
            row.setGenderLabel(b.getGenderPolicy() == BuildingGenderPolicy.MALE ? "Nam" : "Nữ");
            row.setActive(Boolean.TRUE.equals(b.getActive()));

            if (Boolean.TRUE.equals(b.getActive())) {
                row.setStatusLabel("Hoạt động");
                row.setStatusClass("is-active");
            } else {
                row.setStatusLabel("Tạm tắt");
                row.setStatusClass("is-inactive");
            }

            // Zone calculation (e.g. Khu A, Khu B, Khu C)
            String zoneName = "Khu " + b.getCode();
            row.setZone(zoneName);
            row.setZoneClass(getZoneClass(b.getCode()));

            // Room metrics
            List<Room> rooms = roomsByBuilding.getOrDefault(b.getId(), List.of());
            Set<Integer> distinctFloors = new HashSet<>();
            int maxFloor = 0;
            for (Room r : rooms) {
                if (r.getFloor() != null) {
                    distinctFloors.add(r.getFloor());
                    if (r.getFloor() > maxFloor) {
                        maxFloor = r.getFloor();
                    }
                }
            }
            row.setRoomCount(rooms.size());
            row.setFloorCount(maxFloor > 0 ? maxFloor : (distinctFloors.isEmpty() ? 1 : distinctFloors.size()));

            // Bed metrics
            List<Bed> beds = bedsByBuilding.getOrDefault(b.getId(), List.of());
            int occupiedBeds = 0;
            int vacantBeds = 0;
            int maintBeds = 0;
            for (Bed bed : beds) {
                if (bed.getStatus() == BedStatus.OCCUPIED) {
                    occupiedBeds++;
                } else if (bed.getStatus() == BedStatus.VACANT) {
                    vacantBeds++;
                } else {
                    maintBeds++;
                }
            }
            row.setBedCount(beds.size());
            row.setOccupiedBeds(occupiedBeds);
            row.setVacantBeds(vacantBeds);
            row.setMaintenanceBeds(maintBeds);
            row.setOccupancyPercent(percent(occupiedBeds, beds.size()));

            // Address and image
            row.setAddress("Địa chỉ: " + zoneName + ", Trường ĐH XYZ");
            row.setImageUrl(fallbackImages[Math.abs(b.getId() != null ? b.getId().hashCode() : i) % fallbackImages.length]);

            rows.add(row);
        }

        rows.sort(Comparator.comparing(BuildingRowDto::getCode, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    private static String getZoneClass(String code) {
        if (code == null || code.isBlank()) {
            return "is-zone-a";
        }
        char c = Character.toUpperCase(code.charAt(0));
        return switch (c) {
            case 'A', 'D' -> "is-zone-a";
            case 'B', 'E' -> "is-zone-b";
            default -> "is-zone-c";
        };
    }

    private static double percent(long part, long whole) {
        if (whole <= 0) {
            return 0.0;
        }
        return Math.round(part * 1000.0 / whole) / 10.0;
    }

    private static void apply(Building building, BuildingForm form, String code) {
        building.setCode(code);
        building.setName(form.getName().trim());
        building.setGenderPolicy(form.getGenderPolicy());
        building.setActive(form.isActive());
    }

    private static String normalizeCode(String code) {
        return code.trim().toUpperCase();
    }
}
