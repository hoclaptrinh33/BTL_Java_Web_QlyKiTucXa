package com.ktx.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.Notification;
import com.ktx.domain.Room;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.PriorityCategory;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.DashboardSnapshot;
import com.ktx.dto.DashboardSnapshot.BuildingOccupancy;
import com.ktx.dto.DashboardSnapshot.DashboardNotice;
import com.ktx.dto.DashboardSnapshot.RecentApplicationRow;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.NotificationRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.repository.StudentRepository;

@Service
public class DashboardService {

    private static final DateTimeFormatter SUBMITTED_AT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));

    private final StudentRepository studentRepository;
    private final BuildingRepository buildingRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final ContractRepository contractRepository;
    private final RoomApplicationRepository roomApplicationRepository;
    private final NotificationRepository notificationRepository;

    public DashboardService(StudentRepository studentRepository, BuildingRepository buildingRepository,
            RoomRepository roomRepository, BedRepository bedRepository, ContractRepository contractRepository,
            RoomApplicationRepository roomApplicationRepository, NotificationRepository notificationRepository) {
        this.studentRepository = studentRepository;
        this.buildingRepository = buildingRepository;
        this.roomRepository = roomRepository;
        this.bedRepository = bedRepository;
        this.contractRepository = contractRepository;
        this.roomApplicationRepository = roomApplicationRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSnapshot load() {
        DashboardSnapshot snap = new DashboardSnapshot();
        List<Building> buildings = buildingRepository.findAll();
        List<Room> rooms = roomRepository.findAllWithBuilding();
        List<Bed> beds = bedRepository.findAllWithRoomAndBuilding();

        snap.setStudentCount(studentRepository.count());
        snap.setOccupyingStudentCount(contractRepository.countByStatusIn(OccupyingStatuses.OCCUPYING));
        snap.setBuildingCount(buildings.size());
        snap.setRoomCount(rooms.size());
        snap.setActiveRoomCount(rooms.stream().filter(r -> r.getStatus() == RoomStatus.ACTIVE).count());
        snap.setMaintenanceRooms(rooms.stream().filter(r -> r.getStatus() == RoomStatus.MAINTENANCE).count());

        Map<Long, int[]> bedsByRoom = new HashMap<>();
        Map<Long, BuildingOccupancy> byBuilding = new HashMap<>();
        for (Building building : buildings) {
            BuildingOccupancy row = new BuildingOccupancy();
            row.setCode(building.getCode());
            row.setName(building.getName());
            row.setGenderLabel(building.getGenderPolicy() == BuildingGenderPolicy.MALE ? "Nam" : "Nữ");
            byBuilding.put(building.getId(), row);
        }
        for (Room room : rooms) {
            BuildingOccupancy row = byBuilding.get(room.getBuilding().getId());
            if (row != null) {
                row.setRooms(row.getRooms() + 1);
            }
        }

        long occupied = 0;
        long vacant = 0;
        long maintenance = 0;
        for (Bed bed : beds) {
            Room room = bed.getRoom();
            if (room.getStatus() == RoomStatus.INACTIVE) {
                continue;
            }
            int[] counts = bedsByRoom.computeIfAbsent(room.getId(), id -> new int[3]);
            BedStatus status = bed.getStatus();
            if (status == BedStatus.OCCUPIED) {
                occupied++;
                counts[0]++;
            } else if (status == BedStatus.VACANT) {
                vacant++;
                counts[1]++;
            } else {
                maintenance++;
                counts[2]++;
            }
            BuildingOccupancy row = byBuilding.get(room.getBuilding().getId());
            if (row != null) {
                if (status == BedStatus.OCCUPIED) {
                    row.setOccupied(row.getOccupied() + 1);
                } else if (status == BedStatus.VACANT) {
                    row.setVacant(row.getVacant() + 1);
                } else {
                    row.setMaintenance(row.getMaintenance() + 1);
                }
            }
        }

        long emptyRooms = 0;
        long fullRooms = 0;
        for (Room room : rooms) {
            if (room.getStatus() != RoomStatus.ACTIVE) {
                continue;
            }
            int[] counts = bedsByRoom.get(room.getId());
            if (counts == null) {
                continue;
            }
            int total = counts[0] + counts[1] + counts[2];
            if (total > 0 && counts[1] == total) {
                emptyRooms++;
            }
            if (total > 0 && counts[0] == total) {
                fullRooms++;
            }
        }

        snap.setOccupiedBeds(occupied);
        snap.setVacantBeds(vacant);
        snap.setMaintenanceBeds(maintenance);
        snap.setEmptyRooms(emptyRooms);
        snap.setFullRooms(fullRooms);
        snap.setOccupancyPercent(percent(occupied, occupied + vacant));

        byBuilding.values().stream()
                .sorted(Comparator.comparing(BuildingOccupancy::getCode, String.CASE_INSENSITIVE_ORDER))
                .forEach(row -> {
                    row.setOccupancyPercent(percent(row.getOccupied(), row.getOccupied() + row.getVacant()));
                    snap.getBuildings().add(row);
                });

        mapApplications(snap);
        mapNotices(snap);
        return snap;
    }

    private void mapApplications(DashboardSnapshot snap) {
        for (RoomApplication app : roomApplicationRepository.findRecent(PageRequest.of(0, 6))) {
            Student student = app.getStudent();
            RecentApplicationRow row = new RecentApplicationRow();
            row.setId(app.getId());
            row.setCode("ĐN-" + String.format("%06d", app.getId() == null ? 0 : app.getId()));
            row.setStudentName(student.getFullName());
            row.setStudentCode(student.getStudentCode());
            row.setInitials(initials(student.getFullName()));
            row.setSubmittedAt(app.getSubmittedAt() == null ? "—" : SUBMITTED_AT.format(app.getSubmittedAt()));
            row.setRoomTypeLabel(roomTypeLabel(app.getPreferredRoomType()));
            row.setPriorityLabel(priorityLabel(app.getPrioritySnapshot()));
            row.setPriorityTone(priorityTone(app.getPrioritySnapshot()));
            row.setStatusLabel(applicationStatusLabel(app.getStatus()));
            row.setStatusTone(applicationStatusTone(app.getStatus()));
            snap.getRecentApplications().add(row);
        }
    }

    private void mapNotices(DashboardSnapshot snap) {
        long unread = notificationRepository.countByReadFlagFalse();
        snap.setUnreadNotifications((int) Math.min(unread, Integer.MAX_VALUE));
        List<Notification> stored = notificationRepository.findTop5ByOrderByCreatedAtDesc();
        if (!stored.isEmpty()) {
            for (Notification n : stored) {
                snap.getNotices().add(new DashboardNotice("sky", n.getTitle(), n.getBody(), relativeTime(n.getCreatedAt())));
            }
            return;
        }
        if (snap.getBuildingCount() == 0) {
            snap.getNotices().add(new DashboardNotice("rose", "Chưa có tòa nhà",
                    "Thêm tòa Nam/Nữ trước khi mở đợt đăng ký.", "Hệ thống"));
        }
        if (snap.getStudentCount() == 0) {
            snap.getNotices().add(new DashboardNotice("peach", "Chưa có sinh viên",
                    "Sinh viên tự đăng ký. Danh sách nội trú hiện trống.", "Hệ thống"));
        }
        if (snap.getCapacityBeds() > 0 && snap.getOccupiedBeds() == 0) {
            snap.getNotices().add(new DashboardNotice("sky", "Chưa phân bổ chỗ ở",
                    "Mở đợt và chạy preview trước khi chốt giường.", "Hệ thống"));
        }
        if (snap.getNotices().isEmpty()) {
            snap.getNotices().add(new DashboardNotice("mint", "Hệ thống sẵn sàng",
                    "Chưa có thông báo vận hành mới.", "Hôm nay"));
        }
    }

    private static double percent(long part, long whole) {
        if (whole <= 0) {
            return 0;
        }
        return Math.round(part * 1000.0 / whole) / 10.0;
    }

    static String initials(String name) {
        if (name == null || name.isBlank()) {
            return "SV";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase(Locale.ROOT);
    }

    static String roomTypeLabel(RoomType type) {
        if (type == null) {
            return "Chưa chọn";
        }
        return switch (type) {
            case STANDARD_4 -> "Phòng 4 người";
            case STANDARD_6 -> "Phòng 6 người";
            case STANDARD_8 -> "Phòng 8 người";
            case VIP_AC -> "Phòng VIP";
        };
    }

    static String priorityLabel(PriorityCategory category) {
        if (category == null || category == PriorityCategory.NONE) {
            return "Thường";
        }
        return category == PriorityCategory.POLICY ? "Chính sách" : "Vùng sâu";
    }

    static String priorityTone(PriorityCategory category) {
        if (category == PriorityCategory.POLICY) {
            return "rose";
        }
        if (category == PriorityCategory.REMOTE_AREA) {
            return "peach";
        }
        return "muted";
    }

    static String applicationStatusLabel(ApplicationStatus status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case DRAFT -> "Nháp";
            case SUBMITTED -> "Đã nộp";
            case ALLOCATED -> "Đã xếp";
            case WAITLISTED -> "Chờ chỗ";
            case REJECTED -> "Từ chối";
            case WITHDRAWN -> "Đã rút";
        };
    }

    static String applicationStatusTone(ApplicationStatus status) {
        if (status == null) {
            return "muted";
        }
        return switch (status) {
            case SUBMITTED -> "violet";
            case ALLOCATED -> "mint";
            case WAITLISTED -> "peach";
            case REJECTED, WITHDRAWN, DRAFT -> "muted";
        };
    }

    private static String relativeTime(LocalDateTime at) {
        if (at == null) {
            return "";
        }
        Duration d = Duration.between(at, LocalDateTime.now());
        if (d.toMinutes() < 1) {
            return "Vừa xong";
        }
        if (d.toMinutes() < 60) {
            return d.toMinutes() + " phút trước";
        }
        if (d.toHours() < 24) {
            return d.toHours() + " giờ trước";
        }
        return d.toDays() + " ngày trước";
    }
}
