package com.ktx.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Room;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.dto.RoomBatchForm;
import com.ktx.dto.RoomBatchResult;
import com.ktx.dto.RoomForm;
import com.ktx.dto.RoomRow;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.domain.Contract;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.dto.OccupancyDriftRow;

@Service
public class RoomService {

    public static final String NOT_FOUND = "Không tìm thấy phòng";
    public static final String BUILDING_NOT_FOUND = "Không tìm thấy tòa";
    public static final String DUPLICATE_NUMBER = "Số phòng đã có trong tòa này";
    public static final String CANNOT_MAINTENANCE =
            "Không chuyển bảo trì khi còn giường đang có người";
    public static final String CANNOT_DELETE_OCCUPIED = "Không xóa phòng còn giường đang có người";
    public static final String TYPE_LOCKED = "Không đổi loại phòng khi đã có giường";
    public static final String FLOOR_RANGE_INVALID = "Tầng đến phải ≥ tầng từ";
    public static final String BATCH_TOO_LARGE = "Tối đa 100 phòng mỗi lần sinh";
    public static final String BATCH_EMPTY = "Không có phòng mới — mọi số trong dải đã tồn tại";
    public static final int MAX_BATCH_ROOMS = 100;

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final BedRepository bedRepository;
    private final RoomAssetRepository roomAssetRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final ContractRepository contractRepository;

    public RoomService(RoomRepository roomRepository, BuildingRepository buildingRepository,
            BedRepository bedRepository, RoomAssetRepository roomAssetRepository,
            SystemConfigRepository systemConfigRepository, ContractRepository contractRepository) {
        this.roomRepository = roomRepository;
        this.buildingRepository = buildingRepository;
        this.bedRepository = bedRepository;
        this.roomAssetRepository = roomAssetRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.contractRepository = contractRepository;
    }

    @Transactional(readOnly = true)
    public List<Room> list(Long buildingId) {
        if (buildingId != null) {
            return roomRepository.findByBuildingIdWithBuilding(buildingId);
        }
        return roomRepository.findAllWithBuilding();
    }

    @Transactional(readOnly = true)
    public List<RoomRow> listRows(Long buildingId) {
        List<Room> rooms = list(buildingId);
        List<Bed> beds = bedRepository.findAllWithRoomAndBuilding();
        Map<Long, long[]> counts = new HashMap<>();
        for (Bed bed : beds) {
            long[] c = counts.computeIfAbsent(bed.getRoom().getId(), id -> new long[3]);
            if (bed.getStatus() == BedStatus.OCCUPIED) {
                c[0]++;
            } else if (bed.getStatus() == BedStatus.VACANT) {
                c[1]++;
            } else {
                c[2]++;
            }
        }
        List<RoomRow> rows = new ArrayList<>();
        for (Room room : rooms) {
            rows.add(toRow(room, counts.getOrDefault(room.getId(), new long[3])));
        }
        return rows;
    }

    public static RoomRow toRow(Room room, long[] bedCounts) {
        RoomRow row = new RoomRow();
        row.setId(room.getId());
        Building building = room.getBuilding();
        row.setBuildingId(building.getId());
        row.setBuildingCode(building.getCode());
        row.setBuildingName(building.getName());
        row.setGenderLabel(building.getGenderPolicy() == BuildingGenderPolicy.MALE ? "Nam" : "Nữ");
        row.setRoomNumber(room.getRoomNumber());
        row.setDoorCode(building.getCode() + "-" + room.getRoomNumber());
        row.setFloor(room.getFloor());
        row.setRoomType(room.getRoomType());
        row.setTypeLabel(typeLabel(room.getRoomType()));
        row.setCapacity(room.getCapacity());
        row.setPricePerTerm(room.getPricePerTerm());
        row.setStatus(room.getStatus());
        row.setStatusLabel(statusLabel(room.getStatus()));
        row.setStatusClass(switch (room.getStatus()) {
            case ACTIVE -> "is-active";
            case MAINTENANCE -> "is-maintenance";
            case INACTIVE -> "is-inactive";
        });
        row.setOccupiedBeds(bedCounts[0]);
        row.setVacantBeds(bedCounts[1]);
        row.setMaintenanceBeds(bedCounts[2]);
        return row;
    }

    @Transactional(readOnly = true)
    public Room getById(Long id) {
        return roomRepository.findByIdWithBuilding(id).orElseThrow(() -> new BusinessException(NOT_FOUND));
    }

    @Transactional
    public Room create(RoomForm form) {
        Building building = buildingRepository.findById(form.getBuildingId())
                .orElseThrow(() -> new BusinessException(BUILDING_NOT_FOUND));
        String number = normalizeNumber(form.getRoomNumber());
        if (roomRepository.existsByBuildingIdAndRoomNumber(building.getId(), number)) {
            throw new DuplicateFieldException("roomNumber", DUPLICATE_NUMBER);
        }
        int capacity = capacityOf(form.getRoomType());
        Room room = new Room();
        room.setBuilding(building);
        room.setRoomNumber(number);
        room.setFloor(form.getFloor());
        room.setRoomType(form.getRoomType());
        room.setCapacity(capacity);
        room.setPricePerTerm(form.getPricePerTerm());
        room.setStatus(form.getStatus() == null ? RoomStatus.ACTIVE : form.getStatus());
        room = roomRepository.save(room);
        createBeds(room, capacity);
        return room;
    }

    @Transactional
    public RoomBatchResult createBatch(RoomBatchForm form) {
        Building building = buildingRepository.findById(form.getBuildingId())
                .orElseThrow(() -> new BusinessException(BUILDING_NOT_FOUND));
        int floorFrom = form.getFloorFrom();
        int floorTo = form.getFloorTo();
        int perFloor = form.getRoomsPerFloor();
        if (floorTo < floorFrom) {
            throw new BusinessException(FLOOR_RANGE_INVALID);
        }
        int planned = (floorTo - floorFrom + 1) * perFloor;
        if (planned > MAX_BATCH_ROOMS) {
            throw new BusinessException(BATCH_TOO_LARGE);
        }
        int capacity = capacityOf(form.getRoomType());
        RoomStatus status = form.getStatus() == null ? RoomStatus.ACTIVE : form.getStatus();
        Set<String> existing = new HashSet<>(roomRepository.findRoomNumbersByBuildingId(building.getId()));

        List<Room> toCreate = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        for (int floor = floorFrom; floor <= floorTo; floor++) {
            for (int seq = 1; seq <= perFloor; seq++) {
                String number = roomNumberOf(floor, seq);
                if (existing.contains(number)) {
                    skipped.add(number);
                    continue;
                }
                existing.add(number);
                Room room = new Room();
                room.setBuilding(building);
                room.setRoomNumber(number);
                room.setFloor(floor);
                room.setRoomType(form.getRoomType());
                room.setCapacity(capacity);
                room.setPricePerTerm(form.getPricePerTerm());
                room.setStatus(status);
                toCreate.add(room);
            }
        }
        if (toCreate.isEmpty()) {
            throw new BusinessException(BATCH_EMPTY);
        }

        List<Room> saved = roomRepository.saveAll(toCreate);
        List<Bed> beds = new ArrayList<>();
        for (Room room : saved) {
            for (int i = 1; i <= capacity; i++) {
                Bed bed = new Bed();
                bed.setRoom(room);
                bed.setBedCode("G" + i);
                bed.setStatus(BedStatus.VACANT);
                bed.setVersion(0L);
                beds.add(bed);
            }
        }
        bedRepository.saveAll(beds);

        RoomBatchResult result = new RoomBatchResult();
        result.setBuildingId(building.getId());
        result.setBuildingCode(building.getCode());
        result.setCreated(saved.size());
        result.setSkipped(skipped.size());
        result.setBedsCreated(beds.size());
        result.getSkippedNumbers().addAll(skipped);
        Room first = saved.getFirst();
        Room last = saved.get(saved.size() - 1);
        result.setFirstDoorCode(building.getCode() + "-" + first.getRoomNumber());
        result.setLastDoorCode(building.getCode() + "-" + last.getRoomNumber());
        return result;
    }

    public static String roomNumberOf(int floor, int sequence) {
        return String.valueOf(floor * 100 + sequence);
    }

    @Transactional
    public Room update(Long id, RoomForm form) {
        Room room = getById(id);
        String number = normalizeNumber(form.getRoomNumber());
        if (roomRepository.existsByBuildingIdAndRoomNumberAndIdNot(room.getBuilding().getId(), number, id)) {
            throw new DuplicateFieldException("roomNumber", DUPLICATE_NUMBER);
        }
        if (form.getRoomType() != room.getRoomType()) {
            if (bedRepository.countByRoomId(id) > 0) {
                throw new BusinessException(TYPE_LOCKED);
            }
            room.setRoomType(form.getRoomType());
            room.setCapacity(capacityOf(form.getRoomType()));
        }
        if (form.getStatus() == RoomStatus.MAINTENANCE
                && bedRepository.countByRoomIdAndStatus(id, BedStatus.OCCUPIED) > 0) {
            throw new BusinessException(CANNOT_MAINTENANCE);
        }
        room.setRoomNumber(number);
        room.setFloor(form.getFloor());
        room.setPricePerTerm(form.getPricePerTerm());
        room.setStatus(form.getStatus());
        return roomRepository.save(room);
    }

    @Transactional
    public void delete(Long id) {
        Room room = getById(id);
        if (bedRepository.countByRoomIdAndStatus(id, BedStatus.OCCUPIED) > 0) {
            throw new BusinessException(CANNOT_DELETE_OCCUPIED);
        }
        roomAssetRepository.deleteAll(roomAssetRepository.findByRoomIdOrderByIdAsc(id));
        List<Bed> beds = bedRepository.findByRoomIdOrderByBedCodeAsc(id);
        bedRepository.deleteAll(beds);
        roomRepository.delete(room);
    }

    public int capacityOf(RoomType type) {
        if (type == null) {
            return 0;
        }
        return switch (type) {
            case STANDARD_4 -> 4;
            case STANDARD_6 -> 6;
            case STANDARD_8 -> 8;
            case VIP_AC -> vipCapacity();
        };
    }

    public static BigDecimal defaultPrice(RoomType type) {
        if (type == null) {
            return BigDecimal.ZERO;
        }
        return switch (type) {
            case STANDARD_8 -> new BigDecimal("1200000");
            case STANDARD_6 -> new BigDecimal("1800000");
            case STANDARD_4 -> new BigDecimal("2400000");
            case VIP_AC -> new BigDecimal("4000000");
        };
    }

    public static String typeLabel(RoomType type) {
        if (type == null) {
            return "—";
        }
        return switch (type) {
            case STANDARD_4 -> "Phòng 4 giường";
            case STANDARD_6 -> "Phòng 6 giường";
            case STANDARD_8 -> "Phòng 8 giường";
            case VIP_AC -> "Phòng VIP";
        };
    }

    public static String statusLabel(RoomStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case ACTIVE -> "Hoạt động";
            case MAINTENANCE -> "Bảo trì";
            case INACTIVE -> "Ngưng dùng";
        };
    }

    private int vipCapacity() {
        return systemConfigRepository.findById("room.type.vip.capacity")
                .map(SystemConfig::getConfigValue)
                .map(v -> {
                    try {
                        return Integer.parseInt(v.trim());
                    } catch (NumberFormatException ex) {
                        return 2;
                    }
                })
                .orElse(2);
    }

    private void createBeds(Room room, int capacity) {
        for (int i = 1; i <= capacity; i++) {
            Bed bed = new Bed();
            bed.setRoom(room);
            bed.setBedCode("G" + i);
            bed.setStatus(BedStatus.VACANT);
            bed.setVersion(0L);
            bedRepository.save(bed);
        }
    }

    private static String normalizeNumber(String raw) {
        return raw == null ? "" : raw.trim().toUpperCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<OccupancyDriftRow> findOccupancyDrifts() {
        List<Bed> beds = bedRepository.findAllWithRoomAndBuilding();
        List<Contract> contracts = contractRepository.findOccupyingWithDetails(OccupyingStatuses.OCCUPYING);

        Map<Long, Contract> bedActiveContractMap = new HashMap<>();
        for (Contract c : contracts) {
            if (c.getBed() != null) {
                bedActiveContractMap.put(c.getBed().getId(), c);
            }
        }

        List<OccupancyDriftRow> drifts = new ArrayList<>();
        for (Bed bed : beds) {
            Contract activeContract = bedActiveContractMap.get(bed.getId());
            boolean isDrift = false;

            if (activeContract != null) {
                if (bed.getStatus() != BedStatus.OCCUPIED 
                        || bed.getCurrentContractId() == null 
                        || !bed.getCurrentContractId().equals(activeContract.getId())) {
                    isDrift = true;
                }
            } else {
                if (bed.getStatus() == BedStatus.OCCUPIED || bed.getCurrentContractId() != null) {
                    isDrift = true;
                }
            }

            if (isDrift) {
                OccupancyDriftRow row = new OccupancyDriftRow();
                row.setBedId(bed.getId());
                row.setBuildingCode(bed.getRoom().getBuilding().getCode());
                row.setRoomNumber(bed.getRoom().getRoomNumber());
                row.setBedCode(bed.getBedCode());
                row.setActualStatus(bed.getStatus().name());
                row.setActualContractId(bed.getCurrentContractId());

                if (activeContract != null) {
                    row.setExpectedStatus(BedStatus.OCCUPIED.name());
                    row.setExpectedContractId(activeContract.getId());
                    row.setExpectedContractNo(activeContract.getContractNo());
                    if (activeContract.getStudent() != null) {
                        row.setStudentName(activeContract.getStudent().getFullName());
                        row.setStudentCode(activeContract.getStudent().getStudentCode());
                    }
                } else {
                    row.setExpectedStatus(bed.getStatus() == BedStatus.MAINTENANCE ? BedStatus.MAINTENANCE.name() : BedStatus.VACANT.name());
                    row.setExpectedContractId(null);
                    row.setExpectedContractNo("—");
                    row.setStudentName("—");
                    row.setStudentCode("—");
                }
                drifts.add(row);
            }
        }
        return drifts;
    }

    @Transactional
    public void reconcileOccupancy() {
        List<Bed> beds = bedRepository.findAll();
        List<Contract> contracts = contractRepository.findOccupyingWithDetails(OccupyingStatuses.OCCUPYING);

        Map<Long, Contract> bedActiveContractMap = new HashMap<>();
        for (Contract c : contracts) {
            if (c.getBed() != null) {
                bedActiveContractMap.put(c.getBed().getId(), c);
            }
        }

        List<Bed> toUpdate = new ArrayList<>();
        for (Bed bed : beds) {
            Contract activeContract = bedActiveContractMap.get(bed.getId());
            boolean modified = false;

            if (activeContract != null) {
                if (bed.getStatus() != BedStatus.OCCUPIED) {
                    bed.setStatus(BedStatus.OCCUPIED);
                    modified = true;
                }
                if (bed.getCurrentContractId() == null || !bed.getCurrentContractId().equals(activeContract.getId())) {
                    bed.setCurrentContractId(activeContract.getId());
                    modified = true;
                }
            } else {
                if (bed.getStatus() == BedStatus.OCCUPIED) {
                    bed.setStatus(BedStatus.VACANT);
                    modified = true;
                }
                if (bed.getCurrentContractId() != null) {
                    bed.setCurrentContractId(null);
                    modified = true;
                }
            }

            if (modified) {
                toUpdate.add(bed);
            }
        }

        if (!toUpdate.isEmpty()) {
            bedRepository.saveAll(toUpdate);
        }
    }

    @Transactional
    public void reconcileSingleBed(Long bedId) {
        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy giường"));
        
        List<Contract> activeContracts = contractRepository.findOccupyingWithDetails(OccupyingStatuses.OCCUPYING);
        Contract activeContractForBed = null;
        for (Contract c : activeContracts) {
            if (c.getBed() != null && c.getBed().getId().equals(bedId)) {
                activeContractForBed = c;
                break;
            }
        }

        boolean modified = false;
        if (activeContractForBed != null) {
            if (bed.getStatus() != BedStatus.OCCUPIED) {
                bed.setStatus(BedStatus.OCCUPIED);
                modified = true;
            }
            if (bed.getCurrentContractId() == null || !bed.getCurrentContractId().equals(activeContractForBed.getId())) {
                bed.setCurrentContractId(activeContractForBed.getId());
                modified = true;
            }
        } else {
            if (bed.getStatus() == BedStatus.OCCUPIED) {
                bed.setStatus(BedStatus.VACANT);
                modified = true;
            }
            if (bed.getCurrentContractId() != null) {
                bed.setCurrentContractId(null);
                modified = true;
            }
        }

        if (modified) {
            bedRepository.save(bed);
        }
    }
}
