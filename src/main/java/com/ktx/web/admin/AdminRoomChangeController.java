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
import com.ktx.domain.Bed;
import com.ktx.domain.Building;
import com.ktx.domain.RoomChangeRequest;
import com.ktx.domain.enums.DepositStatus;
import com.ktx.domain.enums.RoomChangeKind;
import com.ktx.domain.enums.RoomChangeStatus;
import com.ktx.repository.BedRepository;
import com.ktx.repository.BuildingRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.service.RoomChangeService;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAnyAuthority('contract.read', 'contract.write')")
public class AdminRoomChangeController {

    private final RoomChangeService roomChangeService;
    private final BedRepository bedRepository;
    private final BuildingRepository buildingRepository;

    public AdminRoomChangeController(RoomChangeService roomChangeService,
                                     BedRepository bedRepository,
                                     BuildingRepository buildingRepository) {
        this.roomChangeService = roomChangeService;
        this.bedRepository = bedRepository;
        this.buildingRepository = buildingRepository;
    }

    private String base() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null && attrs.getRequest() != null && attrs.getRequest().getRequestURI() != null) {
                return attrs.getRequest().getRequestURI().startsWith("/manage") ? "/manage/room-changes" : "/admin/room-changes";
            }
        } catch (Exception ignored) {
        }
        return "/admin/room-changes";
    }

    @GetMapping({"/manage/room-changes", "/admin/room-changes"})
    public String list(@RequestParam(value = "kind", required = false) RoomChangeKind kind,
                       @RequestParam(value = "status", required = false) RoomChangeStatus status,
                       @RequestParam(value = "buildingId", required = false) Long buildingId,
                       Model model) {
        List<RoomChangeRequest> requests = roomChangeService.searchRequests(kind, status, buildingId);
        List<Bed> vacantBeds = bedRepository.findVacantBedsWithDetails();
        List<Building> buildings = buildingRepository.findAll();

        model.addAttribute("requests", requests);
        model.addAttribute("vacantBeds", vacantBeds);
        model.addAttribute("buildings", buildings);
        model.addAttribute("kinds", RoomChangeKind.values());
        model.addAttribute("statuses", RoomChangeStatus.values());
        model.addAttribute("depositStatuses", DepositStatus.values());
        model.addAttribute("selectedKind", kind);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedBuildingId", buildingId);
        model.addAttribute("pageTitle", "Quản lý Đổi / Trả phòng");
        model.addAttribute("pageSubtitle", "Xử lý nguyện vọng chuyển phòng và thủ tục trả phòng của sinh viên");
        model.addAttribute("activeMenu", "room-changes");
        return "admin/room_changes/list";
    }

    @PostMapping({"/manage/room-changes/{id}/approve", "/admin/room-changes/{id}/approve"})
    public String approveRoomChange(@PathVariable("id") Long id,
                                    @RequestParam("targetBedId") Long targetBedId,
                                    @RequestParam(value = "adminNote", required = false) String adminNote,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            Long adminUserId = getUserId(auth);
            roomChangeService.approveAndExecuteRoomChange(id, targetBedId, adminUserId, adminNote);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Duyệt đổi phòng thành công! Giường cũ đã được giải phóng và hợp đồng chuyển sang giường mới.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi duyệt chuyển phòng: " + ex.getMessage());
        }
        return "redirect:" + base();
    }

    @PostMapping({"/manage/room-changes/{id}/checkout", "/admin/room-changes/{id}/checkout"})
    public String approveReturnRoom(@PathVariable("id") Long id,
                                    @RequestParam(value = "assetNote", required = false) String assetNote,
                                    @RequestParam(value = "ok", defaultValue = "true") Boolean ok,
                                    @RequestParam(value = "depositDecision", required = false) DepositStatus depositDecision,
                                    @RequestParam(value = "force", defaultValue = "false") boolean force,
                                    Authentication auth,
                                    RedirectAttributes redirectAttributes) {
        try {
            boolean canForce = auth != null && auth.getAuthorities().stream().anyMatch(a ->
                    "checkout.force".equals(a.getAuthority())
                    || "ROLE_ADMIN".equals(a.getAuthority())
                    || "ROLE_QUAN_LY".equals(a.getAuthority()));

            if (force && !canForce) {
                throw new org.springframework.security.access.AccessDeniedException("Bạn không có quyền thực hiện trả phòng cưỡng chế (checkout.force)");
            }

            Long adminUserId = getUserId(auth);
            roomChangeService.approveReturnRoom(id, adminUserId, assetNote, ok, depositDecision, force, null);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Duyệt trả phòng và thực hiện Check-out thành công! Giường đã được giải phóng về VACANT và hợp đồng chuyển sang COMPLETED.");
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            throw ex;
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xử lý trả phòng: " + ex.getMessage());
        }
        return "redirect:" + base();
    }

    @PostMapping({"/manage/room-changes/{id}/reject", "/admin/room-changes/{id}/reject"})
    public String rejectRequest(@PathVariable("id") Long id,
                                @RequestParam(value = "adminNote", required = false) String adminNote,
                                Authentication auth,
                                RedirectAttributes redirectAttributes) {
        try {
            Long adminUserId = getUserId(auth);
            roomChangeService.rejectRequest(id, adminUserId, adminNote);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn yêu cầu thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi từ chối yêu cầu: " + ex.getMessage());
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