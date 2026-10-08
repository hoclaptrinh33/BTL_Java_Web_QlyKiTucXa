package com.ktx.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ktx.domain.Building;
import com.ktx.domain.BuildingImage;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.RoomImage;
import com.ktx.domain.User;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BuildingImageRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.repository.RoomImageRepository;
import com.ktx.repository.UserRepository;
import com.ktx.service.FileStorageService;

@Component
@Order(100) // Runs after DataSeeder
public class MediaSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MediaSeeder.class);

    private final BuildingRepository buildingRepository;
    private final BuildingImageRepository buildingImageRepository;
    private final RoomImageRepository roomImageRepository;
    private final RegistrationPeriodRepository registrationPeriodRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public MediaSeeder(BuildingRepository buildingRepository,
                       BuildingImageRepository buildingImageRepository,
                       RoomImageRepository roomImageRepository,
                       RegistrationPeriodRepository registrationPeriodRepository,
                       UserRepository userRepository,
                       FileStorageService fileStorageService) {
        this.buildingRepository = buildingRepository;
        this.buildingImageRepository = buildingImageRepository;
        this.roomImageRepository = roomImageRepository;
        this.registrationPeriodRepository = registrationPeriodRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        try {
            seedBuildingImages();
            seedRoomImages();
            seedRegistrationPeriods();
        } catch (Exception e) {
            log.error("Error running MediaSeeder: {}", e.getMessage(), e);
        }
    }

    private void seedRegistrationPeriods() {
        if (registrationPeriodRepository.count() > 0) {
            return;
        }

        User adminUser = userRepository.findByUsername("quanly")
                .orElseGet(() -> userRepository.findByUsername("admin")
                        .orElseGet(() -> {
                            List<User> all = userRepository.findAll();
                            return all.isEmpty() ? null : all.get(0);
                        }));

        if (adminUser == null) {
            log.warn("MediaSeeder: No admin user found to associate with registration periods.");
            return;
        }

        RegistrationPeriod p1 = new RegistrationPeriod();
        p1.setName("Đăng ký Chỗ ở Tân Sinh viên K2026 — Học kỳ 1");
        p1.setPeriodType(PeriodType.FRESHMAN);
        p1.setAcademicYear("2026-2027");
        p1.setOpenAt(LocalDateTime.now().minusDays(2));
        p1.setCloseAt(LocalDateTime.now().plusDays(20));
        p1.setTermStart(LocalDate.of(2026, 9, 1));
        p1.setTermEnd(LocalDate.of(2027, 1, 15));
        p1.setStatus(PeriodStatus.OPEN);
        p1.setCreatedBy(adminUser);
        registrationPeriodRepository.save(p1);

        RegistrationPeriod p2 = new RegistrationPeriod();
        p2.setName("Đăng ký Lưu trú Năm học 2026-2027 (Sinh viên Khóa cũ)");
        p2.setPeriodType(PeriodType.NEW_ACADEMIC_YEAR);
        p2.setAcademicYear("2026-2027");
        p2.setOpenAt(LocalDateTime.now().minusDays(1));
        p2.setCloseAt(LocalDateTime.now().plusDays(10));
        p2.setTermStart(LocalDate.of(2026, 9, 1));
        p2.setTermEnd(LocalDate.of(2027, 1, 15));
        p2.setStatus(PeriodStatus.OPEN);
        p2.setCreatedBy(adminUser);
        registrationPeriodRepository.save(p2);

        log.info("MediaSeeder: Seeded 2 active registration periods successfully.");
    }

    private void seedBuildingImages() throws IOException {
        if (buildingImageRepository.count() > 0) {
            return;
        }

        List<Building> buildings = buildingRepository.findAll();
        if (buildings.isEmpty()) {
            return;
        }

        Path bldgDir = fileStorageService.getRootUploadPath().resolve("buildings");
        Files.createDirectories(bldgDir);

        for (Building b : buildings) {
            String code = b.getCode() != null ? b.getCode() : "X";
            String name = b.getName() != null ? b.getName() : "Tòa " + code;
            boolean isFemale = b.getGenderPolicy() == BuildingGenderPolicy.FEMALE;
            String primaryColor = isFemale ? "#e11d48" : "#2563eb";
            String secondaryColor = isFemale ? "#fda4af" : "#93c5fd";

            // 1. Mặt tiền chính
            String svg1 = generateBuildingSvg(code, name, "Cổng chính & Mặt tiền", primaryColor, secondaryColor, "🏢");
            String fileName1 = "bldg-" + code.toLowerCase() + "-main.svg";
            Files.writeString(bldgDir.resolve(fileName1), svg1, StandardCharsets.UTF_8);

            BuildingImage img1 = new BuildingImage(b, "/uploads/buildings/" + fileName1, "Mặt tiền chính & sảnh đón tiếp", true, 1);
            buildingImageRepository.save(img1);

            // 2. Khuôn viên & Sân thể thao
            String svg2 = generateBuildingSvg(code, name, "Khuôn viên & Cây xanh", "#059669", "#6ee7b7", "🌳");
            String fileName2 = "bldg-" + code.toLowerCase() + "-yard.svg";
            Files.writeString(bldgDir.resolve(fileName2), svg2, StandardCharsets.UTF_8);

            BuildingImage img2 = new BuildingImage(b, "/uploads/buildings/" + fileName2, "Khuôn viên và đường nội khu", false, 2);
            buildingImageRepository.save(img2);

            // 3. Nhà xe & Tiện ích
            String svg3 = generateBuildingSvg(code, name, "Nhà xe & An ninh 24/7", "#4f46e5", "#c7d2fe", "🛵");
            String fileName3 = "bldg-" + code.toLowerCase() + "-parking.svg";
            Files.writeString(bldgDir.resolve(fileName3), svg3, StandardCharsets.UTF_8);

            BuildingImage img3 = new BuildingImage(b, "/uploads/buildings/" + fileName3, "Khu vực để xe và chốt an ninh", false, 3);
            buildingImageRepository.save(img3);
        }

        log.info("MediaSeeder: Seeded building images successfully.");
    }

    private void seedRoomImages() throws IOException {
        if (roomImageRepository.count() > 0) {
            return;
        }

        Path roomDir = fileStorageService.getRootUploadPath().resolve("rooms");
        Files.createDirectories(roomDir);

        for (RoomType rt : RoomType.values()) {
            String title;
            String color;
            switch (rt) {
                case STANDARD_4 -> {
                    title = "Phòng 4 giường tiêu chuẩn";
                    color = "#2563eb";
                }
                case STANDARD_6 -> {
                    title = "Phòng 6 giường tiêu chuẩn";
                    color = "#0891b2";
                }
                case STANDARD_8 -> {
                    title = "Phòng 8 giường tiết kiệm";
                    color = "#d97706";
                }
                case VIP_AC -> {
                    title = "Phòng VIP Cao cấp";
                    color = "#7c3aed";
                }
                default -> {
                    title = rt.name();
                    color = "#475569";
                }
            }

            // 1. Không gian tổng thể
            String svg1 = generateRoomSvg(title, "Toàn cảnh phòng & Giường ngủ", color, "🛏️");
            String fn1 = "room-" + rt.name().toLowerCase() + "-overview.svg";
            Files.writeString(roomDir.resolve(fn1), svg1, StandardCharsets.UTF_8);
            roomImageRepository.save(new RoomImage(rt, null, null, "/uploads/rooms/" + fn1, "Toàn cảnh phòng & giường tầng", true, 1));

            // 2. Góc học tập
            String svg2 = generateRoomSvg(title, "Bàn học & Tủ đồ cá nhân", "#059669", "📚");
            String fn2 = "room-" + rt.name().toLowerCase() + "-desk.svg";
            Files.writeString(roomDir.resolve(fn2), svg2, StandardCharsets.UTF_8);
            roomImageRepository.save(new RoomImage(rt, null, null, "/uploads/rooms/" + fn2, "Góc bàn học cá nhân & giá sách", false, 2));

            // 3. Khu vệ sinh
            String svg3 = generateRoomSvg(title, "Khu vệ sinh & Ban công", "#0284c7", "🚿");
            String fn3 = "room-" + rt.name().toLowerCase() + "-bath.svg";
            Files.writeString(roomDir.resolve(fn3), svg3, StandardCharsets.UTF_8);
            roomImageRepository.save(new RoomImage(rt, null, null, "/uploads/rooms/" + fn3, "Phòng tắm & ban công phơi đồ", false, 3));
        }

        log.info("MediaSeeder: Seeded room type images successfully.");
    }

    private String generateBuildingSvg(String code, String name, String subtitle, String c1, String c2, String icon) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 800 500\" width=\"100%\" height=\"100%\">\n" +
                "  <defs>\n" +
                "    <linearGradient id=\"bg\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">\n" +
                "      <stop offset=\"0%\" stop-color=\"" + c1 + "\"/>\n" +
                "      <stop offset=\"100%\" stop-color=\"" + c2 + "\"/>\n" +
                "    </linearGradient>\n" +
                "  </defs>\n" +
                "  <rect width=\"800\" height=\"500\" fill=\"url(#bg)\"/>\n" +
                "  <circle cx=\"400\" cy=\"200\" r=\"110\" fill=\"rgba(255,255,255,0.15)\"/>\n" +
                "  <text x=\"400\" y=\"225\" font-size=\"90\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\">" + icon + "</text>\n" +
                "  <rect x=\"80\" y=\"330\" width=\"640\" height=\"120\" rx=\"16\" fill=\"rgba(0,0,0,0.35)\"/>\n" +
                "  <text x=\"400\" y=\"375\" font-size=\"32\" font-weight=\"bold\" fill=\"#ffffff\" text-anchor=\"middle\" font-family=\"Segoe UI, Roboto, sans-serif\">Tòa " + code + " — " + name + "</text>\n" +
                "  <text x=\"400\" y=\"415\" font-size=\"20\" fill=\"#f8fafc\" text-anchor=\"middle\" font-family=\"Segoe UI, Roboto, sans-serif\">" + subtitle + "</text>\n" +
                "</svg>";
    }

    private String generateRoomSvg(String title, String subtitle, String color, String icon) {
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 800 500\" width=\"100%\" height=\"100%\">\n" +
                "  <defs>\n" +
                "    <linearGradient id=\"roomBg\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">\n" +
                "      <stop offset=\"0%\" stop-color=\"" + color + "\"/>\n" +
                "      <stop offset=\"100%\" stop-color=\"#0f172a\"/>\n" +
                "    </linearGradient>\n" +
                "  </defs>\n" +
                "  <rect width=\"800\" height=\"500\" fill=\"url(#roomBg)\"/>\n" +
                "  <circle cx=\"400\" cy=\"190\" r=\"100\" fill=\"rgba(255,255,255,0.12)\"/>\n" +
                "  <text x=\"400\" y=\"215\" font-size=\"80\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\">" + icon + "</text>\n" +
                "  <rect x=\"80\" y=\"320\" width=\"640\" height=\"130\" rx=\"16\" fill=\"rgba(15,23,42,0.65)\"/>\n" +
                "  <text x=\"400\" y=\"370\" font-size=\"30\" font-weight=\"bold\" fill=\"#ffffff\" text-anchor=\"middle\" font-family=\"Segoe UI, Roboto, sans-serif\">" + title + "</text>\n" +
                "  <text x=\"400\" y=\"410\" font-size=\"19\" fill=\"#94a3b8\" text-anchor=\"middle\" font-family=\"Segoe UI, Roboto, sans-serif\">" + subtitle + "</text>\n" +
                "</svg>";
    }
}
