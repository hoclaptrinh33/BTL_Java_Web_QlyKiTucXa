package com.ktx.web.staff;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.Building;
import com.ktx.domain.CheckInOut;
import com.ktx.domain.Contract;
import com.ktx.domain.RoomAsset;
import com.ktx.domain.Staff;
import com.ktx.domain.enums.AssetCondition;
import com.ktx.domain.enums.ContractStatus;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.InvoiceStatus;
import com.ktx.repository.BuildingRepository;
import com.ktx.repository.InvoiceRepository;
import com.ktx.repository.RoomAssetRepository;
import com.ktx.repository.StaffRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.security.StaffScope;
import com.ktx.service.CheckInOutService;
import com.ktx.service.ContractService;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'QUAN_LY', 'CAN_BO') or hasAuthority('checkin.operate')")
public class StaffCheckInController {

    private final ContractService contractService;
    private final CheckInOutService checkInOutService;
    private final StaffScope staffScope;
    private final StaffRepository staffRepository;
    private final BuildingRepository buildingRepository;
    private final RoomAssetRepository roomAssetRepository;
    private final InvoiceRepository invoiceRepository;

    public StaffCheckInController(ContractService contractService,
                                  CheckInOutService checkInOutService,
                                  StaffScope staffScope,
                                  StaffRepository staffRepository,
                                  BuildingRepository buildingRepository,
                                  RoomAssetRepository roomAssetRepository,
                                  InvoiceRepository invoiceRepository) {
        this.contractService = contractService;
        this.checkInOutService = checkInOutService;
        this.staffScope = staffScope;
        this.staffRepository = staffRepository;
        this.buildingRepository = buildingRepository;
        this.roomAssetRepository = roomAssetRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @GetMapping({"/manage/checkin", "/staff/checkin"})
    public String index(Authentication auth, HttpServletRequest request, Model model) {
        Long buildingId = staffScope.buildingId(auth).orElse(null);
        Building assignedBuilding = null;
        if (buildingId != null) {
            assignedBuilding = buildingRepository.findById(buildingId).orElse(null);
        }

        List<Contract> draftContracts = contractService.findByBuildingAndStatus(buildingId, List.of(ContractStatus.DRAFT));
        List<Contract> checkoutContracts = contractService.findByBuildingAndStatus(buildingId,
                List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRED, ContractStatus.TERMINATED));
        List<CheckInOut> recentHistory = checkInOutService.findRecent(buildingId);

        model.addAttribute("building", assignedBuilding);
        model.addAttribute("draftContracts", draftContracts);
        model.addAttribute("checkoutContracts", checkoutContracts);
        model.addAttribute("recentHistory", recentHistory);
        model.addAttribute("pageTitle", "Quản lý Check-in & Check-out");
        model.addAttribute("pageSubtitle", assignedBuilding != null
                ? "Tòa " + assignedBuilding.getCode() + " — Tiếp nhận sinh viên và bàn giao trả phòng"
                : "Tiếp nhận sinh viên và bàn giao trả phòng KTX");
        model.addAttribute("activeMenu", "checkin");
        model.addAttribute("baseUrl", getBaseUrl(request));
        return "staff/checkin/index";
    }

    @GetMapping({"/manage/checkin/{id}", "/staff/checkin/{id}"})
    public String checkInForm(@PathVariable("id") Long id, Authentication auth, HttpServletRequest request, Model model) {
        Contract contract = contractService.getByIdWithDetails(id);
        staffScope.assertBuilding(auth, contract.getBed().getRoom().getBuilding().getId());

        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new BusinessException("Hợp đồng #" + contract.getContractNo() + " không ở trạng thái DRAFT");
        }

        List<RoomAsset> roomAssets = roomAssetRepository.findByRoomIdOrderByIdAsc(contract.getBed().getRoom().getId());

        model.addAttribute("contract", contract);
        model.addAttribute("roomAssets", roomAssets);
        model.addAttribute("pageTitle", "Thủ tục Check-in — HĐ " + contract.getContractNo());
        model.addAttribute("pageSubtitle", "Kiểm tra danh mục tài sản và xác nhận bàn giao phòng");
        model.addAttribute("activeMenu", "checkin");
        model.addAttribute("baseUrl", getBaseUrl(request));
        return "staff/checkin/form";
    }

    @PostMapping({"/manage/checkin/{id}", "/staff/checkin/{id}"})
    public String doCheckIn(@PathVariable("id") Long id,
                            @RequestParam(value = "assetNote", required = false) String assetNote,
                            @RequestParam(value = "ok", defaultValue = "true") Boolean ok,
                            @RequestParam Map<String, String> allParams,
                            HttpServletRequest request,
                            Authentication auth,
                            RedirectAttributes redirectAttributes) {
        String base = getBaseUrl(request);
        try {
            Contract contract = contractService.getByIdWithDetails(id);
            staffScope.assertBuilding(auth, contract.getBed().getRoom().getBuilding().getId());

            Map<Long, AssetCondition> assetConditions = extractAssetConditions(allParams);
            Long staffUserId = getUserId(auth);

            checkInOutService.checkIn(id, staffUserId, assetNote, ok, assetConditions);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Check-in thành công cho sinh viên " + contract.getStudent().getFullName()
                            + "! Hợp đồng đã ACTIVE và hóa đơn cọc đã được tạo.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + base + "/checkin/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi check-in: " + ex.getMessage());
            return "redirect:" + base + "/checkin/" + id;
        }
        return "redirect:" + base + "/checkin";
    }

    @GetMapping({"/manage/checkout/{id}", "/staff/checkout/{id}"})
    public String checkOutForm(@PathVariable("id") Long id, Authentication auth, HttpServletRequest request, Model model) {
        Contract contract = contractService.getByIdWithDetails(id);
        staffScope.assertBuilding(auth, contract.getBed().getRoom().getBuilding().getId());

        ContractStatus status = contract.getStatus();
        if (status != ContractStatus.ACTIVE && status != ContractStatus.EXPIRED && status != ContractStatus.TERMINATED) {
            throw new BusinessException("Hợp đồng #" + contract.getContractNo() + " không ở trạng thái có thể check-out");
        }

        List<RoomAsset> roomAssets = roomAssetRepository.findByRoomIdOrderByIdAsc(contract.getBed().getRoom().getId());
        boolean hasOverdue = contract.getStudent() != null &&
                invoiceRepository.existsByStudentIdAndStatus(contract.getStudent().getId(), InvoiceStatus.OVERDUE);

        model.addAttribute("contract", contract);
        model.addAttribute("roomAssets", roomAssets);
        model.addAttribute("hasOverdue", hasOverdue);
        model.addAttribute("canForceCheckout", canForceCheckout(auth));
        model.addAttribute("depositStatuses", DepositStatus.values());
        model.addAttribute("pageTitle", "Thủ tục Check-out — HĐ " + contract.getContractNo());
        model.addAttribute("pageSubtitle", "Kiểm tra bàn giao phòng, xử lý tiền cọc và giải phóng chỗ ở");
        model.addAttribute("activeMenu", "checkin");
        model.addAttribute("baseUrl", getBaseUrl(request));
        return "staff/checkin/checkout_form";
    }

    @PostMapping({"/manage/checkout/{id}", "/staff/checkout/{id}"})
    public String doCheckOut(@PathVariable("id") Long id,
                             @RequestParam(value = "assetNote", required = false) String assetNote,
                             @RequestParam(value = "ok", defaultValue = "true") Boolean ok,
                             @RequestParam(value = "depositDecision", required = false) DepositStatus depositDecision,
                             @RequestParam(value = "force", defaultValue = "false") boolean requestedForce,
                             @RequestParam Map<String, String> allParams,
                             HttpServletRequest request,
                             Authentication auth,
                             RedirectAttributes redirectAttributes) {
        String base = getBaseUrl(request);
        try {
            Contract contract = contractService.getByIdWithDetails(id);
            staffScope.assertBuilding(auth, contract.getBed().getRoom().getBuilding().getId());

            Map<Long, AssetCondition> assetConditions = extractAssetConditions(allParams);
            Long staffUserId = getUserId(auth);

            // §04-04 & Req 5: checkout.force chỉ hiện và chỉ gọi được với người có quyền đó (quản lý).
            // Nhân viên không trả phòng khi hóa đơn OVERDUE.
            boolean canForce = canForceCheckout(auth);
            boolean effectiveForce = canForce && (requestedForce || Boolean.parseBoolean(allParams.get("force")));

            checkInOutService.checkOut(id, staffUserId, assetNote, ok, depositDecision, effectiveForce, assetConditions);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Check-out thành công cho sinh viên " + contract.getStudent().getFullName()
                            + "! Giường đã được giải phóng (VACANT).");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + base + "/checkout/" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi check-out: " + ex.getMessage());
            return "redirect:" + base + "/checkout/" + id;
        }
        return "redirect:" + base + "/checkin";
    }

    private String getBaseUrl(HttpServletRequest request) {
        if (request == null || request.getRequestURI() == null) {
            return "/manage";
        }
        return request.getRequestURI().startsWith("/staff") ? "/staff" : "/manage";
    }

    private boolean canForceCheckout(Authentication auth) {
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream().anyMatch(a ->
                "checkout.force".equals(a.getAuthority()) ||
                "ROLE_ADMIN".equals(a.getAuthority()) ||
                "ROLE_QUAN_LY".equals(a.getAuthority())
        );
    }

    private Map<Long, AssetCondition> extractAssetConditions(Map<String, String> params) {
        Map<Long, AssetCondition> result = new HashMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("asset_condition_")) {
                try {
                    Long assetId = Long.parseLong(entry.getKey().substring("asset_condition_".length()));
                    AssetCondition cond = AssetCondition.valueOf(entry.getValue());
                    result.put(assetId, cond);
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof KtxUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        throw new BusinessException("Không xác định được danh tính người dùng");
    }
}
