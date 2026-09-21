package com.ktx.web.student;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.util.OccupyingStatuses;
import com.ktx.domain.Contract;
import com.ktx.domain.Student;
import com.ktx.repository.ContractRepository;
import com.ktx.repository.StudentRepository;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import com.ktx.domain.Building;
import com.ktx.domain.RenewalRequest;
import com.ktx.domain.RoomChangeRequest;
import com.ktx.domain.enums.RenewalStatus;
import com.ktx.domain.enums.RoomChangeKind;
import com.ktx.domain.enums.RoomChangeStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.repository.BuildingRepository;
import com.ktx.service.RenewalService;
import com.ktx.service.RoomApplicationService;
import com.ktx.service.RoomChangeService;

@Controller
@RequestMapping("/student")
public class StudentContractController {

    private final StudentRepository studentRepository;
    private final ContractRepository contractRepository;
    private final com.ktx.repository.CheckInOutRepository checkInOutRepository;
    private final com.ktx.repository.InvoiceRepository invoiceRepository;
    private final com.ktx.repository.RoomAssetRepository roomAssetRepository;
    private final RoomChangeService roomChangeService;
    private final RenewalService renewalService;
    private final BuildingRepository buildingRepository;

    public StudentContractController(StudentRepository studentRepository,
                                     ContractRepository contractRepository,
                                     com.ktx.repository.CheckInOutRepository checkInOutRepository,
                                     com.ktx.repository.InvoiceRepository invoiceRepository,
                                     com.ktx.repository.RoomAssetRepository roomAssetRepository,
                                     RoomChangeService roomChangeService,
                                     RenewalService renewalService,
                                     BuildingRepository buildingRepository) {
        this.studentRepository = studentRepository;
        this.contractRepository = contractRepository;
        this.checkInOutRepository = checkInOutRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomAssetRepository = roomAssetRepository;
        this.roomChangeService = roomChangeService;
        this.renewalService = renewalService;
        this.buildingRepository = buildingRepository;
    }

    @GetMapping("/contract")
    public String viewContract(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        List<com.ktx.domain.CheckInOut> checkInOutList = List.of();
        List<com.ktx.domain.Invoice> invoices = List.of();
        List<com.ktx.domain.RoomAsset> roomAssets = List.of();
        if (activeContract != null) {
            checkInOutList = checkInOutRepository.findByContractIdOrderByPerformedAtDesc(activeContract.getId());
            invoices = invoiceRepository.findByContractIdOrderByDueDateDesc(activeContract.getId());
            if (activeContract.getBed() != null && activeContract.getBed().getRoom() != null) {
                roomAssets = roomAssetRepository.findByRoomIdOrderByIdAsc(activeContract.getBed().getRoom().getId());
            }
        }

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("checkInOutList", checkInOutList);
        model.addAttribute("invoices", invoices);
        model.addAttribute("roomAssets", roomAssets);
        model.addAttribute("pageTitle", "Hợp đồng lưu trú");
        model.addAttribute("pageSubtitle", "Thông tin hợp đồng và quy chế phòng ở");
        model.addAttribute("activeMenu", "contract");
        return "student/contract/detail";
    }

    @GetMapping("/renewals")
    public String renewals(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        List<RenewalRequest> requests = renewalService.findByStudentId(student.getId());
        boolean hasPendingRenewal = requests.stream()
                .anyMatch(r -> r.getStatus() == RenewalStatus.SUBMITTED);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("requests", requests);
        model.addAttribute("hasPendingRenewal", hasPendingRenewal);
        model.addAttribute("pageTitle", "Gia hạn hợp đồng");
        model.addAttribute("pageSubtitle", "Nộp đơn xin ở tiếp sang học kỳ mới");
        model.addAttribute("activeMenu", "renewals");
        return "student/contract/renewals";
    }

    @PostMapping("/renewals")
    public String submitRenewal(@RequestParam(value = "termMonths", defaultValue = "5") Integer termMonths,
                                @RequestParam(value = "requestedEnd", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate requestedEnd,
                                @RequestParam(value = "note", required = false) String note,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            Student student = getStudent(principal);
            renewalService.submitRenewal(student.getId(), termMonths, requestedEnd, note);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã gửi đơn xin gia hạn hợp đồng thành công! Vui lòng chờ BQL xét duyệt.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi đơn gia hạn: " + ex.getMessage());
        }
        return "redirect:/student/renewals";
    }

    @PostMapping("/renewals/{id}/cancel")
    public String cancelRenewal(@PathVariable("id") Long id,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            Student student = getStudent(principal);
            renewalService.cancelRenewal(id, student.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn xin gia hạn hợp đồng thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hủy đơn: " + ex.getMessage());
        }
        return "redirect:/student/renewals";
    }

    @GetMapping("/room-change")
    public String roomChange(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        List<RoomChangeRequest> requests = roomChangeService.findByStudentIdAndKind(student.getId(), RoomChangeKind.CHANGE);
        List<Building> buildings = buildingRepository.findAll();

        boolean hasPendingChange = requests.stream()
                .anyMatch(r -> r.getStatus() == RoomChangeStatus.SUBMITTED);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("requests", requests);
        model.addAttribute("buildings", buildings);
        model.addAttribute("hasPendingChange", hasPendingChange);
        model.addAttribute("pageTitle", "Yêu cầu chuyển phòng");
        model.addAttribute("pageSubtitle", "Đăng ký đổi sang phòng hoặc giường khác");
        model.addAttribute("activeMenu", "room-change");
        return "student/contract/room-change";
    }

    @PostMapping("/room-change")
    public String submitRoomChange(@RequestParam("targetRoomType") String targetRoomType,
                                   @RequestParam("reason") String reason,
                                   @RequestParam(value = "preferredBuildingId", required = false) Long preferredBuildingId,
                                   @RequestParam(value = "preferredBuilding", required = false) String preferredBuilding,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            Student student = getStudent(principal);
            Long resolvedBuildingId = preferredBuildingId;
            if (resolvedBuildingId == null && preferredBuilding != null && !preferredBuilding.isBlank()) {
                try {
                    resolvedBuildingId = Long.parseLong(preferredBuilding.trim());
                } catch (NumberFormatException ignored) {
                    resolvedBuildingId = buildingRepository.findAll().stream()
                            .filter(b -> b.getCode().equalsIgnoreCase(preferredBuilding.trim()) || b.getName().equalsIgnoreCase(preferredBuilding.trim()))
                            .map(Building::getId)
                            .findFirst().orElse(null);
                }
            }

            RoomType roomType = null;
            if (targetRoomType != null && !targetRoomType.isBlank()) {
                try {
                    roomType = RoomType.valueOf(targetRoomType.trim());
                } catch (IllegalArgumentException ignored) {
                }
            }

            roomChangeService.submitRoomChangeRequest(student.getId(), resolvedBuildingId, roomType, reason);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã nộp đơn xin chuyển phòng thành công! Vui lòng đợi BQL ký túc xá xét duyệt.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi đơn: " + ex.getMessage());
        }
        return "redirect:/student/room-change";
    }

    @PostMapping("/room-change/{id}/cancel")
    public String cancelRoomChange(@PathVariable("id") Long id,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            Student student = getStudent(principal);
            roomChangeService.cancelRequest(id, student.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn xin chuyển phòng thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hủy đơn: " + ex.getMessage());
        }
        return "redirect:/student/room-change";
    }

    @GetMapping("/return-room")
    public String returnRoom(Principal principal, Model model) {
        Student student = getStudent(principal);
        List<Contract> contracts = contractRepository.findByStudentIdAndStatusInWithDetails(
                student.getId(), OccupyingStatuses.OCCUPYING);
        Contract activeContract = contracts.isEmpty() ? null : contracts.get(0);

        List<RoomChangeRequest> requests = roomChangeService.findByStudentIdAndKind(student.getId(), RoomChangeKind.RETURN);
        boolean hasPendingReturn = requests.stream()
                .anyMatch(r -> r.getStatus() == RoomChangeStatus.SUBMITTED);

        model.addAttribute("student", student);
        model.addAttribute("contract", activeContract);
        model.addAttribute("requests", requests);
        model.addAttribute("hasPendingReturn", hasPendingReturn);
        model.addAttribute("pageTitle", "Yêu cầu trả phòng");
        model.addAttribute("pageSubtitle", "Thủ tục thanh lý hợp đồng và bàn giao chỗ ở");
        model.addAttribute("activeMenu", "return-room");
        return "student/contract/return-room";
    }

    @PostMapping("/return-room")
    public String submitReturnRoom(@RequestParam("returnDate") String returnDate,
                                   @RequestParam("reason") String reason,
                                   @RequestParam("bankAccount") String bankAccount,
                                   @RequestParam("bankName") String bankName,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            Student student = getStudent(principal);
            roomChangeService.submitReturnRoomRequest(student.getId(), returnDate, reason, bankName, bankAccount);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã nộp đơn đăng ký trả phòng thành công! Cán bộ quản lý sẽ liên hệ kiểm tra tài sản và hoàn tất thủ tục check-out.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi gửi đơn trả phòng: " + ex.getMessage());
        }
        return "redirect:/student/return-room";
    }

    @PostMapping("/return-room/{id}/cancel")
    public String cancelReturnRoom(@PathVariable("id") Long id,
                                   Principal principal,
                                   RedirectAttributes redirectAttributes) {
        try {
            Student student = getStudent(principal);
            roomChangeService.cancelRequest(id, student.getId());
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy đơn đăng ký trả phòng thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hủy đơn: " + ex.getMessage());
        }
        return "redirect:/student/return-room";
    }

    private Student getStudent(Principal principal) {
        return studentRepository.findByUserUsername(principal.getName())
                .orElseThrow(() -> new BusinessException(RoomApplicationService.STUDENT_NOT_FOUND));
    }
}
