package com.ktx.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Bed;
import com.ktx.domain.Room;
import com.ktx.domain.RoomApplication;
import com.ktx.domain.Student;
import com.ktx.domain.SystemConfig;
import com.ktx.domain.AllocationItem;
import com.ktx.domain.enums.AllocationResult;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.AllocConfig;
import com.ktx.dto.AllocationRunResult;
import com.ktx.repository.BedRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.repository.SystemConfigRepository;
import com.ktx.service.AllocationEngine;
import com.ktx.service.OccupancySnapshot;

@Service
public class AllocationEngineImpl implements AllocationEngine {

    private final RoomApplicationRepository roomApplicationRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final BedRepository bedRepository;
    private final ContractRepository contractRepository;

    @Autowired
    public AllocationEngineImpl(RoomApplicationRepository roomApplicationRepository,
                                SystemConfigRepository systemConfigRepository,
                                BedRepository bedRepository,
                                ContractRepository contractRepository) {
        this.roomApplicationRepository = roomApplicationRepository;
        this.systemConfigRepository = systemConfigRepository;
        this.bedRepository = bedRepository;
        this.contractRepository = contractRepository;
    }

    @Override
    public AllocationRunResult plan(long periodId) {
        // 1. Tải cấu hình config hiện tại
        AllocConfig cfg = loadConfig();

        // 2. Lấy toàn bộ đơn ở trạng thái SUBMITTED của đợt
        List<RoomApplication> apps = roomApplicationRepository.findByPeriodIdAndStatus(periodId, ApplicationStatus.SUBMITTED);

        // 3. Tính điểm số và xếp hạng deterministic
        List<ScoredApplication> scoredApps = apps.stream()
                .map(a -> new ScoredApplication(a, scoreOf(a, cfg)))
                .sorted(scoredAppComparator)
                .collect(Collectors.toList());

        // 4. Khởi tạo snapshot in-memory
        OccupancySnapshot snap = OccupancySnapshot.from(bedRepository, contractRepository);

        List<AllocationItem> items = new ArrayList<>();
        int rank = 0;

        for (ScoredApplication sa : scoredApps) {
            rank++;
            RoomApplication app = sa.app();
            Student sv = app.getStudent();

            AllocationItem item = new AllocationItem();
            item.setApplication(app);
            item.setStudent(sv);
            item.setRankNo(rank);
            item.setScore(sa.score());

            // a. Kiểm tra cấm ở
            if (Boolean.TRUE.equals(sv.getBlockedFromHousing())) {
                item.setResult(AllocationResult.SKIPPED);
                item.setReason("SKIPPED_BLOCKED");
                items.add(item);
                continue;
            }

            // b. Kiểm tra đang có hợp đồng chiếm giữ
            if (contractRepository.existsByStudentIdAndStatusIn(sv.getId(), OccupyingStatuses.OCCUPYING)) {
                item.setResult(AllocationResult.SKIPPED);
                item.setReason("SKIPPED_ALREADY_HOUSED");
                items.add(item);
                continue;
            }

            // c. Lọc giường trống khớp nguyện vọng
            List<Bed> candidates = snap.vacantMatching(sv, app, cfg);
            String lastReason = "NO_VACANT_BED";

            if (candidates.isEmpty()) {
                if (cfg.softPreference()) {
                    // Chế độ SOFT: Nới lỏng tòa -> Loại phòng -> Bất kỳ
                    List<Bed> relaxed = snap.relaxBuildingThenType(sv, app, cfg);
                    if (!relaxed.isEmpty()) {
                        candidates = relaxed;
                        lastReason = determineFailureReason(sv, app, snap);
                    } else {
                        lastReason = "NO_VACANT_BED";
                    }
                } else {
                    // Chế độ STRICT: Sai nguyện vọng lập tức vào hàng đợi
                    lastReason = determineFailureReason(sv, app, snap);
                }
            }

            // d. Chọn giường tốt nhất trong số candidates bằng roomAwareComparator
            Optional<Bed> chosen = candidates.stream()
                    .min(roomAwareComparator(sv, snap));

            if (chosen.isEmpty()) {
                item.setResult(AllocationResult.WAITLISTED);
                item.setReason(lastReason);
            } else {
                item.setResult(AllocationResult.ASSIGNED);
                item.setBed(chosen.get());
                // Nếu phải dùng giường nới lỏng, ghi nhận cảnh báo
                if (!isExactMatch(chosen.get(), app)) {
                    item.setReason(determineRelaxationReasonForBed(chosen.get(), app));
                }
                snap.occupy(chosen.get(), sv);
            }
            items.add(item);
        }

        return new AllocationRunResult(items);
    }

    private AllocConfig loadConfig() {
        int policy = Integer.parseInt(systemConfigRepository.findById("alloc.weight.policy")
                .map(SystemConfig::getConfigValue).orElse("1000"));
        int remote = Integer.parseInt(systemConfigRepository.findById("alloc.weight.remote")
                .map(SystemConfig::getConfigValue).orElse("500"));
        int prevGood = Integer.parseInt(systemConfigRepository.findById("alloc.weight.prev_good")
                .map(SystemConfig::getConfigValue).orElse("200"));
        String mode = systemConfigRepository.findById("alloc.preference.mode")
                .map(SystemConfig::getConfigValue).orElse("SOFT");
        return new AllocConfig(policy, remote, prevGood, mode);
    }

    private int scoreOf(RoomApplication a, AllocConfig cfg) {
        int score = 0;
        if (a.getPrioritySnapshot() != null) {
            switch (a.getPrioritySnapshot()) {
                case POLICY -> score += cfg.getPolicyWeight();
                case REMOTE_AREA -> score += cfg.getRemoteWeight();
                case NONE -> score += 0;
            }
        }
        if (Boolean.TRUE.equals(a.getPreviousStayGoodSnapshot())) {
            score += cfg.getPrevGoodWeight();
        }
        return score;
    }

    private String determineFailureReason(Student sv, RoomApplication app, OccupancySnapshot snap) {
        if (!snap.hasVacantBedsMatchingGender(sv)) {
            return "NO_VACANT_BED";
        }
        if (app.getPreferredBuilding() != null && !snap.hasVacantBedsMatchingBuilding(sv, app.getPreferredBuilding())) {
            return "NO_BUILDING_MATCH";
        }
        if (app.getPreferredRoomType() != null && !snap.hasVacantBedsMatchingRoomType(sv, app.getPreferredRoomType())) {
            return "NO_TYPE_MATCH";
        }
        return "NO_VACANT_BED";
    }

    private boolean isExactMatch(Bed bed, RoomApplication app) {
        if (app.getPreferredBuilding() != null) {
            if (bed.getRoom() == null || bed.getRoom().getBuilding() == null || !bed.getRoom().getBuilding().getId().equals(app.getPreferredBuilding().getId())) {
                return false;
            }
        }
        if (app.getPreferredRoomType() != null) {
            if (bed.getRoom() == null || bed.getRoom().getRoomType() != app.getPreferredRoomType()) {
                return false;
            }
        }
        return true;
    }

    private String determineRelaxationReasonForBed(Bed bed, RoomApplication app) {
        boolean buildingMismatch = false;
        if (app.getPreferredBuilding() != null) {
            if (bed.getRoom() == null || bed.getRoom().getBuilding() == null || !bed.getRoom().getBuilding().getId().equals(app.getPreferredBuilding().getId())) {
                buildingMismatch = true;
            }
        }
        boolean typeMismatch = false;
        if (app.getPreferredRoomType() != null) {
            if (bed.getRoom() == null || bed.getRoom().getRoomType() != app.getPreferredRoomType()) {
                typeMismatch = true;
            }
        }
        if (buildingMismatch) {
            return "NO_BUILDING_MATCH";
        }
        if (typeMismatch) {
            return "NO_TYPE_MATCH";
        }
        return null;
    }

    // Helper records & comparators
    private record ScoredApplication(RoomApplication app, int score) {}

    private final Comparator<ScoredApplication> scoredAppComparator = Comparator
            .comparingInt(ScoredApplication::score).reversed()
            .thenComparing(sa -> sa.app().getSubmittedAt(), Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(sa -> sa.app().getStudent() != null ? sa.app().getStudent().getId() : Long.MAX_VALUE);

    private Comparator<Bed> roomAwareComparator(Student sv, OccupancySnapshot snap) {
        return Comparator
                .comparingInt((Bed b) -> roomBonus(b.getRoom(), sv, snap)).reversed()
                .thenComparing(b -> parseRoomNumber(b.getRoom()))
                .thenComparing(b -> b.getBedCode() == null ? "" : b.getBedCode())
                .thenComparing(b -> b.getId() == null ? 0L : b.getId());
    }

    private int roomBonus(Room r, Student sv, OccupancySnapshot snap) {
        if (r == null || sv == null) {
            return 0;
        }
        var occupants = snap.occupants(r);
        if (occupants == null || occupants.isEmpty()) {
            return 0;
        }
        boolean sameClass = occupants.stream()
                .anyMatch(o -> o != null && eq(sv.getClassCode(), o.getClassCode()));
        boolean sameFac = occupants.stream()
                .anyMatch(o -> o != null && eq(sv.getFacultyCode(), o.getFacultyCode()));
        int pack = occupants.size() * 5;
        if (sameClass) return 100 + pack;
        if (sameFac) return 40 + pack;
        return pack;
    }

    private static boolean eq(String a, String b) {
        return a != null && b != null && a.equals(b);
    }

    private static final int NON_NUMERIC_LAST = Integer.MAX_VALUE;
    private record RoomSort(int numeric, String raw) implements Comparable<RoomSort> {
        @Override
        public int compareTo(RoomSort o) {
            int c = Integer.compare(numeric, o.numeric);
            return c != 0 ? c : raw.compareTo(o.raw);
        }
    }

    private RoomSort parseRoomNumber(Room r) {
        if (r == null || r.getRoomNumber() == null) {
            return new RoomSort(NON_NUMERIC_LAST, "");
        }
        String s = r.getRoomNumber().trim();
        try {
            return new RoomSort(Integer.parseInt(s), s);
        } catch (NumberFormatException e) {
            return new RoomSort(NON_NUMERIC_LAST, s);
        }
    }
}
