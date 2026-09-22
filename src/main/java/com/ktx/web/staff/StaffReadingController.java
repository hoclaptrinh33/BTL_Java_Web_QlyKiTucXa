package com.ktx.web.staff;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Building;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.Room;
import com.ktx.domain.UtilityReading;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.domain.enums.InvoiceType;
import com.ktx.dto.RoomReadingDto;
import com.ktx.dto.UtilityReadingForm;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.RoomRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.BillingEngine;
import com.ktx.service.UtilityReadingService;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'QUAN_LY', 'CAN_BO') or hasAnyAuthority('meter.read', 'invoice.issue')")
public class StaffReadingController {

    private final UtilityReadingService utilityReadingService;
    private final BillingEngine billingEngine;
    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final StaffScope staffScope;

    public StaffReadingController(UtilityReadingService utilityReadingService,
                                  BillingEngine billingEngine,
                                  RoomRepository roomRepository,
                                  BuildingRepository buildingRepository,
                                  InvoiceRepository invoiceRepository,
                                  ContractRepository contractRepository,
                                  StaffScope staffScope) {
        this.utilityReadingService = utilityReadingService;
        this.billingEngine = billingEngine;
        this.roomRepository = roomRepository;
        this.buildingRepository = buildingRepository;
        this.invoiceRepository = invoiceRepository;
        this.contractRepository = contractRepository;
        this.staffScope = staffScope;
    }

    @GetMapping("/admin/readings")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminReadingsRedirect(@RequestParam(value = "buildingId", required = false) Long buildingId,
                                        @RequestParam(value = "month", required = false) String month) {
        StringBuilder sb = new StringBuilder("redirect:/staff/readings");
        boolean hasParam = false;
        if (buildingId != null) {
            sb.append("?buildingId=").append(buildingId);
            hasParam = true;
        }
        if (month != null && !month.isBlank()) {
            sb.append(hasParam ? "&" : "?").append("month=").append(month);
        }
        return sb.toString();
    }

    @GetMapping({"/manage/readings", "/staff/readings"})
    public String listReadings(@RequestParam(value = "buildingId", required = false) Long buildingId,
                               @RequestParam(value = "month", required = false) String monthParam,
                               Authentication auth,
                               Model model) {
        boolean admin = isAdmin(auth);
        Long effectiveBuildingId = buildingId;

        if (!admin) {
            effectiveBuildingId = staffScope.buildingId(auth)
                    .orElseThrow(() -> new AccessDeniedException(StaffScope.DENIED_STAFF));
        } else if (effectiveBuildingId == null) {
            List<Building> allBuildings = buildingRepository.findAll();
            if (!allBuildings.isEmpty()) {
                effectiveBuildingId = allBuildings.get(0).getId();
            }
        }

        YearMonth ym;
        if (monthParam != null && !monthParam.isBlank()) {
            try {
                ym = YearMonth.parse(monthParam.trim());
            } catch (DateTimeParseException e) {
                ym = YearMonth.now();
            }
        } else {
            ym = YearMonth.now();
        }

        Building building = null;
        List<RoomReadingDto> roomReadingDtos = new ArrayList<>();
        int recordedCount = 0;
        int issuedCount = 0;

        if (effectiveBuildingId != null) {
            building = buildingRepository.findById(effectiveBuildingId).orElse(null);
            if (building != null) {
                if (!admin) {
                    staffScope.assertBuilding(auth, building.getId());
                }

                List<Room> rooms = roomRepository.findByBuildingIdWithBuilding(building.getId());
                LocalDate billingDate = ym.atDay(1);

                for (Room room : rooms) {
                    RoomReadingDto dto = new RoomReadingDto();
                    dto.setRoomId(room.getId());
                    dto.setRoomNumber(room.getRoomNumber());
                    dto.setFloor(room.getFloor());
                    dto.setRoomTypeName(room.getRoomType() != null ? room.getRoomType().name() : "");

                    List<Contract> occupying = contractRepository.findOccupyingByRoomId(room.getId(), OccupyingStatuses.OCCUPYING);
                    dto.setOccupyingCount(occupying.size());

                    Optional<UtilityReading> readingOpt = utilityReadingService.getReading(room.getId(), ym);
                    if (readingOpt.isPresent()) {
                        UtilityReading r = readingOpt.get();
                        dto.setReading(r);
                        dto.setHasReading(true);
                        try {
                            dto.setKwh(r.calculateKwh());
                        } catch (Exception ignored) {
                            dto.setKwh(null);
                        }
                        try {
                            dto.setM3(r.calculateM3());
                        } catch (Exception ignored) {
                            dto.setM3(null);
                        }
                        recordedCount++;
                    } else {
                        dto.setHasReading(false);
                    }

                    boolean hasActiveInvoice = invoiceRepository.existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
                            room.getId(), billingDate, InvoiceType.UTILITY, InvoiceStatus.CANCELLED);
                    dto.setHasActiveInvoice(hasActiveInvoice);
                    if (hasActiveInvoice) {
                        issuedCount++;
                    }

                    roomReadingDtos.add(dto);
                }
            }
        }

        model.addAttribute("isAdmin", admin);
        model.addAttribute("buildings", buildingRepository.findAll());
        model.addAttribute("building", building);
        model.addAttribute("selectedMonth", ym.toString());
        model.addAttribute("rooms", roomReadingDtos);
        model.addAttribute("totalRooms", roomReadingDtos.size());
        model.addAttribute("recordedCount", recordedCount);
        model.addAttribute("issuedCount", issuedCount);
        model.addAttribute("pageTitle", "Chỉ số điện nước & Hóa đơn");
        model.addAttribute("pageSubtitle", building != null
                ? "Tòa " + building.getCode() + " — Tháng " + ym
                : "Quản lý chỉ số tiêu thụ điện nước sinh viên");
        model.addAttribute("activeMenu", "readings");

        return "staff/readings/list";
    }

    @GetMapping({"/manage/readings/record", "/staff/readings/record"})
    public String recordReadingForm(@RequestParam("roomId") Long roomId,
                                    @RequestParam(value = "month", required = false) String monthParam,
                                    Authentication auth,
                                    Model model) {
        Room room = roomRepository.findByIdWithBuilding(roomId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng #" + roomId));

        staffScope.assertRoom(auth, room);

        YearMonth ym;
        if (monthParam != null && !monthParam.isBlank()) {
            try {
                ym = YearMonth.parse(monthParam.trim());
            } catch (DateTimeParseException e) {
                ym = YearMonth.now();
            }
        } else {
            ym = YearMonth.now();
        }

        boolean hasActiveInvoice = invoiceRepository.existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
                room.getId(), ym.atDay(1), InvoiceType.UTILITY, InvoiceStatus.CANCELLED);

        UtilityReadingForm form = utilityReadingService.prepareForm(roomId, ym);

        model.addAttribute("room", room);
        model.addAttribute("readingForm", form);
        model.addAttribute("hasActiveInvoice", hasActiveInvoice);
        model.addAttribute("selectedMonth", ym.toString());
        model.addAttribute("pageTitle", "Ghi chỉ số điện nước — Phòng " + room.getRoomNumber());
        model.addAttribute("pageSubtitle", "Tòa " + room.getBuilding().getCode() + " — Tháng " + ym);
        model.addAttribute("activeMenu", "readings");

        return "staff/readings/form";
    }

    @PostMapping({"/manage/readings/record", "/staff/readings/record"})
    public String saveReading(@ModelAttribute("readingForm") UtilityReadingForm form,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        Room room = roomRepository.findByIdWithBuilding(form.getRoomId())
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng #" + form.getRoomId()));

        staffScope.assertRoom(auth, room);

        try {
            utilityReadingService.recordReading(form, auth);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã lưu chỉ số điện nước phòng " + room.getRoomNumber() + " tháng " + form.getBillingMonth() + " thành công!");
            return "redirect:" + readingsBase() + "?buildingId=" + room.getBuilding().getId() + "&month=" + form.getBillingMonth();
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + readingsBase() + "/record?roomId=" + form.getRoomId() + "&month=" + form.getBillingMonth();
        }
    }

    @PostMapping({"/manage/readings/{roomId}/issue", "/staff/readings/{roomId}/issue"})
    public String issueUtilityInvoice(@PathVariable("roomId") Long roomId,
                                      @RequestParam("month") String monthStr,
                                      Authentication auth,
                                      RedirectAttributes redirectAttributes) {
        Room room = roomRepository.findByIdWithBuilding(roomId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy phòng #" + roomId));

        staffScope.assertRoom(auth, room);

        YearMonth ym;
        try {
            ym = YearMonth.parse(monthStr.trim());
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Định dạng tháng không hợp lệ: " + monthStr);
            return "redirect:" + readingsBase() + "?buildingId=" + room.getBuilding().getId();
        }

        try {
            List<Invoice> invoices = billingEngine.issueUtilityInvoices(roomId, ym);
            if (invoices.isEmpty()) {
                redirectAttributes.addFlashAttribute("warningMessage",
                        "Phòng " + room.getRoomNumber() + " không có hợp đồng sinh viên đang ở (OCCUPYING) hoặc hóa đơn tháng này đã được phát hành.");
            } else {
                redirectAttributes.addFlashAttribute("successMessage",
                        "Đã phát hành thành công " + invoices.size() + " hóa đơn điện nước cho sinh viên phòng " + room.getRoomNumber() + " tháng " + monthStr + "!");
            }
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        return "redirect:" + readingsBase() + "?buildingId=" + room.getBuilding().getId() + "&month=" + monthStr;
    }

    @PostMapping({"/manage/invoices/generate", "/admin/invoices/generate"})
    @PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAuthority('invoice.issue')")
    public String generateInvoicesAdmin(@RequestParam(value = "buildingId", required = false) Long buildingId,
                                        @RequestParam(value = "roomId", required = false) Long roomId,
                                        @RequestParam("month") String monthStr,
                                        RedirectAttributes redirectAttributes) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(monthStr.trim());
        } catch (DateTimeParseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Định dạng tháng không hợp lệ: " + monthStr);
            return "redirect:" + readingsBase();
        }

        if (roomId != null) {
            Room room = roomRepository.findByIdWithBuilding(roomId)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy phòng #" + roomId));
            try {
                List<Invoice> invoices = billingEngine.issueUtilityInvoices(roomId, ym);
                if (invoices.isEmpty()) {
                    redirectAttributes.addFlashAttribute("warningMessage",
                            "Phòng " + room.getRoomNumber() + " không có hợp đồng đang ở hoặc hóa đơn đã được lập.");
                } else {
                    redirectAttributes.addFlashAttribute("successMessage",
                            "Đã phát hành " + invoices.size() + " hóa đơn cho phòng " + room.getRoomNumber() + " tháng " + monthStr);
                }
            } catch (BusinessException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            }
            return "redirect:" + readingsBase() + "?buildingId=" + room.getBuilding().getId() + "&month=" + monthStr;
        }

        if (buildingId != null) {
            Building building = buildingRepository.findById(buildingId)
                    .orElseThrow(() -> new BusinessException("Không tìm thấy tòa #" + buildingId));
            List<Room> rooms = roomRepository.findByBuildingIdWithBuilding(buildingId);
            int totalInvoices = 0;
            int roomsProcessed = 0;
            LocalDate billingDate = ym.atDay(1);

            for (Room r : rooms) {
                boolean hasReading = utilityReadingService.getReading(r.getId(), ym).isPresent();
                boolean hasActiveInvoice = invoiceRepository.existsByRoomIdAndBillingMonthAndInvoiceTypeAndStatusNot(
                        r.getId(), billingDate, InvoiceType.UTILITY, InvoiceStatus.CANCELLED);

                if (hasReading && !hasActiveInvoice) {
                    try {
                        List<Invoice> invs = billingEngine.issueUtilityInvoices(r.getId(), ym);
                        if (!invs.isEmpty()) {
                            totalInvoices += invs.size();
                            roomsProcessed++;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã phát hành " + totalInvoices + " hóa đơn điện nước cho " + roomsProcessed + " phòng tại tòa " + building.getCode() + " tháng " + monthStr);
            return "redirect:" + readingsBase() + "?buildingId=" + buildingId + "&month=" + monthStr;
        }

        redirectAttributes.addFlashAttribute("errorMessage", "Cần chọn tòa nhà hoặc phòng để phát hành hóa đơn.");
        return "redirect:" + readingsBase() + "?month=" + monthStr;
    }

    private boolean isAdmin(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                            || "ROLE_QUAN_LY".equals(a.getAuthority())
                            || "invoice.issue".equals(a.getAuthority()));
    }

    private String readingsBase() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                String uri = sra.getRequest().getRequestURI();
                if (uri != null && uri.startsWith("/manage")) {
                    return "/manage/readings";
                }
            }
        } catch (Exception ignored) {}
        return "/staff/readings";
    }
}
