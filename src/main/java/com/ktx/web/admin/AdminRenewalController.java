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
import com.ktx.domain.RenewalRequest;
import com.ktx.domain.enums.RenewalStatus;
import com.ktx.repository.BuildingRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.service.RenewalService;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAnyAuthority('contract.read', 'contract.write')")
public class AdminRenewalController {

    private final RenewalService renewalService;
    private final BuildingRepository buildingRepository;

    public AdminRenewalController(RenewalService renewalService,
                                  BuildingRepository buildingRepository) {
        this.renewalService = renewalService;
        this.buildingRepository = buildingRepository;
    }

    private String base() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null && attrs.getRequest() != null && attrs.getRequest().getRequestURI() != null) {
                return attrs.getRequest().getRequestURI().startsWith("/manage") ? "/manage/renewals" : "/admin/renewals";
            }
        } catch (Exception ignored) {
        }
        return "/admin/renewals";
    }

    @GetMapping({"/manage/renewals", "/admin/renewals"})
    public String list(@RequestParam(value = "status", required = false) RenewalStatus status,
                       @RequestParam(value = "buildingId", required = false) Long buildingId,
                       Model model) {
        List<RenewalRequest> requests = renewalService.searchRequests(status, buildingId);
        List<Building> buildings = buildingRepository.findAll();

        model.addAttribute("requests", requests);
        model.addAttribute("buildings", buildings);
        model.addAttribute("statuses", RenewalStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedBuildingId", buildingId);
        model.addAttribute("pageTitle", "Quản lý Gia hạn hợp đồng");
        model.addAttribute("pageSubtitle", "Xét duyệt đơn xin ở tiếp và gia hạn thời hạn lưu trú của sinh viên");
        model.addAttribute("activeMenu", "renewals");
        return "admin/renewals/list";
    }

    @PostMapping({"/manage/renewals/{id}/approve", "/admin/renewals/{id}/approve"})
    public String approveRenewal(@PathVariable("id") Long id,
                                 @RequestParam(value = "adminNote", required = false) String adminNote,
                                 Authentication auth,
                                 RedirectAttributes redirectAttributes) {
        try {
            Long adminUserId = getUserId(auth);
            renewalService.approveRenewal(id, adminUserId, adminNote);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Duyệt gia hạn hợp đồng thành công! Thời hạn hợp đồng đã được cập nhật và chuyển về ACTIVE.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi duyệt gia hạn: " + ex.getMessage());
        }
        return "redirect:" + base();
    }

    @PostMapping({"/manage/renewals/{id}/reject", "/admin/renewals/{id}/reject"})
    public String rejectRenewal(@PathVariable("id") Long id,
                                @RequestParam(value = "adminNote", required = false) String adminNote,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            Long adminUserId = getUserId(auth);
            renewalService.rejectRenewal(id, adminUserId, adminNote);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã từ chối đơn gia hạn. Hợp đồng chuyển về ACTIVE giữ nguyên thời hạn cũ.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi từ chối gia hạn: " + ex.getMessage());
        }
        return "redirect:" + base();
    }

    private Long getUserId(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof KtxUserDetails userDetails) {
            return userDetails.getUser().getId();
        }
        throw new BusinessException("Không xác định được danh tính người dùng");
    }
}