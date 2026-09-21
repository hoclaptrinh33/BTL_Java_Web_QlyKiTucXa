package com.ktx.web.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Building;
import com.ktx.domain.CheckInOut;
import com.ktx.domain.Contract;
import com.ktx.domain.Invoice;
import com.ktx.domain.RoomAsset;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.service.CheckInOutService;
import com.ktx.service.ContractService;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminContractController {

    private final ContractService contractService;
    private final CheckInOutService checkInOutService;
    private final BuildingRepository buildingRepository;
    private final InvoiceRepository invoiceRepository;
    private final RoomAssetRepository roomAssetRepository;

    public AdminContractController(ContractService contractService,
                                   CheckInOutService checkInOutService,
                                   BuildingRepository buildingRepository,
                                   InvoiceRepository invoiceRepository,
                                   RoomAssetRepository roomAssetRepository) {
        this.contractService = contractService;
        this.checkInOutService = checkInOutService;
        this.buildingRepository = buildingRepository;
        this.invoiceRepository = invoiceRepository;
        this.roomAssetRepository = roomAssetRepository;
    }

    @GetMapping("/admin/contracts")
    public String list(@RequestParam(value = "buildingId", required = false) Long buildingId,
                       @RequestParam(value = "status", required = false) ContractStatus status,
                       @RequestParam(value = "keyword", required = false) String keyword,
                       Model model) {
        List<Contract> contracts = contractService.searchContracts(buildingId, status, keyword);
        List<Building> buildings = buildingRepository.findAll();

        model.addAttribute("contracts", contracts);
        model.addAttribute("buildings", buildings);
        model.addAttribute("statuses", ContractStatus.values());
        model.addAttribute("selectedBuildingId", buildingId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Quản lý hợp đồng lưu trú");
        model.addAttribute("pageSubtitle", "Danh sách hợp đồng lưu trú, trạng thái và tiền cọc");
        model.addAttribute("activeMenu", "contracts");
        return "admin/contracts/list";
    }

    @GetMapping("/admin/contracts/{id}")
    public String detail(@PathVariable("id") Long id, Model model) {
        Contract contract = contractService.getByIdWithDetails(id);
        List<CheckInOut> checkInOutList = checkInOutService.findByContractId(id);
        List<Invoice> invoices = invoiceRepository.findByContractIdOrderByDueDateDesc(id);

        List<RoomAsset> roomAssets = List.of();
        if (contract.getBed() != null && contract.getBed().getRoom() != null) {
            roomAssets = roomAssetRepository.findByRoomIdOrderByIdAsc(contract.getBed().getRoom().getId());
        }

        boolean hasOverdue = contract.getStudent() != null &&
                invoiceRepository.existsByStudentIdAndStatus(contract.getStudent().getId(), InvoiceStatus.OVERDUE);

        model.addAttribute("contract", contract);
        model.addAttribute("checkInOutList", checkInOutList);
        model.addAttribute("invoices", invoices);
        model.addAttribute("roomAssets", roomAssets);
        model.addAttribute("hasOverdue", hasOverdue);
        model.addAttribute("depositStatuses", DepositStatus.values());
        model.addAttribute("pageTitle", "Hợp đồng " + contract.getContractNo());
        model.addAttribute("pageSubtitle", "Chi tiết hồ sơ hợp đồng, bàn giao tài sản và nghĩa vụ tài chính");
        model.addAttribute("activeMenu", "contracts");
        return "admin/contracts/detail";
    }

    @PostMapping("/admin/contracts/{id}/check-in")
    public String checkIn(@PathVariable("id") Long id,
                          @RequestParam(value = "assetNote", required = false) String assetNote,
                          @RequestParam(value = "ok", defaultValue = "true") Boolean ok,
                          Authentication auth,
                          RedirectAttributes redirectAttributes) {
        try {
            Long adminUserId = getUserId(auth);
            checkInOutService.checkIn(id, adminUserId, assetNote, ok, null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Check-in hợp đồng thành công! Hợp đồng đã chuyển sang ACTIVE và hóa đơn cọc (50% giá phòng) đã được phát hành.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi check-in: " + ex.getMessage());
        }
        return "redirect:/admin/contracts/" + id;
    }

    @PostMapping("/admin/contracts/{id}/check-out")
    public String checkOut(@PathVariable("id") Long id,
                           @RequestParam(value = "assetNote", required = false) String assetNote,
                           @RequestParam(value = "ok", defaultValue = "true") Boolean ok,
                           @RequestParam(value = "depositDecision", required = false) DepositStatus depositDecision,
                           @RequestParam(value = "force", defaultValue = "false") boolean force,
                           Authentication auth,
                           RedirectAttributes redirectAttributes) {
        try {
            Long adminUserId = getUserId(auth);
            checkInOutService.checkOut(id, adminUserId, assetNote, ok, depositDecision, force, null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Check-out hợp đồng thành công! Giường đã được trả về trạng thái VACANT.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi check-out: " + ex.getMessage());
        }
        return "redirect:/admin/contracts/" + id;
    }

    @PostMapping("/admin/contracts/{id}/cancel-draft")
    public String cancelDraft(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            contractService.cancelDraft(id);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã hủy hợp đồng DRAFT thành công và nhả giường về VACANT.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hủy hợp đồng: " + ex.getMessage());
        }
        return "redirect:/admin/contracts/" + id;
    }

    @PostMapping("/admin/contracts/{id}/terminate")
    public String terminate(@PathVariable("id") Long id,
                            @RequestParam(value = "forfeitDeposit", defaultValue = "false") boolean forfeitDeposit,
                            RedirectAttributes redirectAttributes) {
        try {
            contractService.terminate(id, forfeitDeposit);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chấm dứt hợp đồng (TERMINATED). Giường vẫn được giữ cho sinh viên cho đến khi hoàn tất thủ tục Check-out.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi chấm dứt hợp đồng: " + ex.getMessage());
        }
        return "redirect:/admin/contracts/" + id;
    }

    @GetMapping("/admin/check-in-out")
    public String checkInOutLog(@RequestParam(value = "buildingId", required = false) Long buildingId,
                                Model model) {
        List<CheckInOut> checkInOutList = checkInOutService.findRecent(buildingId);
        List<Building> buildings = buildingRepository.findAll();

        model.addAttribute("checkInOutList", checkInOutList);
        model.addAttribute("buildings", buildings);
        model.addAttribute("selectedBuildingId", buildingId);
        model.addAttribute("pageTitle", "Lịch sử Check-in / Check-out");
        model.addAttribute("pageSubtitle", "Nhật ký nhận phòng, trả phòng và bàn giao tài sản");
        model.addAttribute("activeMenu", "checkin");
        return "admin/contracts/checkin_log";
    }

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof KtxUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        throw new BusinessException("Không xác định được danh tính người dùng");
    }
}
