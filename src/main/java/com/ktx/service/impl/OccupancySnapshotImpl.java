package com.ktx.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Room;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.AllocConfig;
import com.ktx.repository.BedRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.service.OccupancySnapshot;

public class OccupancySnapshotImpl implements OccupancySnapshot {

    private final List<Bed> allBeds;
    private final Map<Long, Student> bedOccupants = new HashMap<>();

    public OccupancySnapshotImpl(BedRepository bedRepo, ContractRepository contractRepo) {
        // 1. Lấy toàn bộ danh sách giường từ DB
        this.allBeds = bedRepo.findAllWithRoomAndBuilding();

        // 2. Lấy danh sách các hợp đồng đang ở từ DB để cập nhật người ở thực tế ban đầu
        List<Contract> occupyingContracts = contractRepo.findOccupyingWithDetails(OccupyingStatuses.OCCUPYING);
        for (Contract c : occupyingContracts) {
            if (c.getBed() != null && c.getStudent() != null) {
                bedOccupants.put(c.getBed().getId(), c.getStudent());
            }
        }
    }

    @Override
    public List<Bed> vacantMatching(Student s, RoomApplication a, AllocConfig c) {
        return allBeds.stream()
                .filter(b -> isVacant(b))
                .filter(b -> matchesGender(b, s))
                .filter(b -> matchesBuilding(b, a))
                .filter(b -> matchesRoomType(b, a))
                .collect(Collectors.toList());
    }

    @Override
    public List<Bed> relaxBuildingThenType(Student s, RoomApplication a, AllocConfig c) {
        // Bước 1: Nới lỏng tòa (tìm giường cùng loại phòng mong muốn, bất kỳ tòa nào cùng giới tính)
        if (a.getPreferredBuilding() != null) {
            List<Bed> droppedBuildingCandidates = allBeds.stream()
                    .filter(b -> isVacant(b))
                    .filter(b -> matchesGender(b, s))
                    .filter(b -> matchesRoomType(b, a))
                    .collect(Collectors.toList());
            if (!droppedBuildingCandidates.isEmpty()) {
                return droppedBuildingCandidates;
            }
        }

        // Bước 2: Nới lỏng loại phòng (tìm giường cùng tòa mong muốn, bất kỳ loại phòng nào)
        if (a.getPreferredRoomType() != null) {
            List<Bed> droppedRoomTypeCandidates = allBeds.stream()
                    .filter(b -> isVacant(b))
                    .filter(b -> matchesGender(b, s))
                    .filter(b -> matchesBuilding(b, a))
                    .collect(Collectors.toList());
            if (!droppedRoomTypeCandidates.isEmpty()) {
                return droppedRoomTypeCandidates;
            }
        }

        // Bước-3: Nới lỏng cả hai (tìm giường trống bất kỳ cùng giới tính)
        return allBeds.stream()
                .filter(b -> isVacant(b))
                .filter(b -> matchesGender(b, s))
                .collect(Collectors.toList());
    }

    @Override
    public void occupy(Bed b, Student s) {
        bedOccupants.put(b.getId(), s);
    }

    @Override
    public Collection<Student> occupants(Room r) {
        List<Student> students = new ArrayList<>();
        if (r == null || r.getId() == null) {
            return students;
        }
        for (Bed b : allBeds) {
            if (b.getRoom() != null && b.getRoom().getId() != null && b.getRoom().getId().equals(r.getId())) {
                Student s = bedOccupants.get(b.getId());
                if (s != null) {
                    students.add(s);
                }
            }
        }
        return students;
    }

    @Override
    public boolean hasVacantBedsMatchingGender(Student s) {
        return allBeds.stream()
                .filter(b -> isVacant(b))
                .anyMatch(b -> matchesGender(b, s));
    }

    @Override
    public boolean hasVacantBedsMatchingBuilding(Student s, Building b) {
        return allBeds.stream()
                .filter(bed -> isVacant(bed))
                .filter(bed -> matchesGender(bed, s))
                .anyMatch(bed -> bed.getRoom() != null && bed.getRoom().getBuilding() != null && bed.getRoom().getBuilding().getId().equals(b.getId()));
    }

    @Override
    public boolean hasVacantBedsMatchingRoomType(Student s, RoomType t) {
        return allBeds.stream()
                .filter(b -> isVacant(b))
                .filter(b -> matchesGender(b, s))
                .anyMatch(b -> b.getRoom() != null && b.getRoom().getRoomType() == t);
    }

    private boolean isVacant(Bed b) {
        // Phòng phải ở trạng thái ACTIVE
        if (b.getRoom() == null || b.getRoom().getStatus() != RoomStatus.ACTIVE) {
            return false;
        }
        // Giường không được bảo trì
        if (b.getStatus() == BedStatus.MAINTENANCE) {
            return false;
        }
        // Giường chưa có người ở trong bộ nhớ
        return !bedOccupants.containsKey(b.getId());
    }

    private boolean matchesGender(Bed b, Student s) {
        if (b.getRoom() == null || b.getRoom().getBuilding() == null || b.getRoom().getBuilding().getGenderPolicy() == null) {
            return false;
        }
        return b.getRoom().getBuilding().getGenderPolicy().name().equals(s.getGender().name());
    }

    private boolean matchesBuilding(Bed b, RoomApplication a) {
        if (a.getPreferredBuilding() == null) {
            return true;
        }
        if (b.getRoom() == null || b.getRoom().getBuilding() == null) {
            return false;
        }
        return b.getRoom().getBuilding().getId().equals(a.getPreferredBuilding().getId());
    }

    private boolean matchesRoomType(Bed b, RoomApplication a) {
        if (a.getPreferredRoomType() == null) {
            return true;
        }
        if (b.getRoom() == null || b.getRoom().getRoomType() == null) {
            return false;
        }
        return b.getRoom().getRoomType() == a.getPreferredRoomType();
    }
}
