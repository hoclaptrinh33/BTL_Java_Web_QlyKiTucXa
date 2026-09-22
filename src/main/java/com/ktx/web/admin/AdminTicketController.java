package com.ktx.web.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.domain.Building;
import com.ktx.domain.MaintenanceTicket;
import com.ktx.domain.enums.TicketStatus;
import com.ktx.repository.BuildingRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.TicketService;

@Controller
@RequestMapping({"/manage/tickets", "/admin/tickets"})
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'QUAN_LY', 'CAN_BO') or hasAuthority('ticket.handle')")
public class AdminTicketController {

    private final TicketService ticketService;
    private final BuildingRepository buildingRepository;
    private final StaffScope staffScope;

    public AdminTicketController(TicketService ticketService,
                                 BuildingRepository buildingRepository,
                                 @Autowired(required = false) StaffScope staffScope) {
        this.ticketService = ticketService;
        this.buildingRepository = buildingRepository;
        this.staffScope = staffScope;
    }

    @GetMapping
    public String listTickets(@RequestParam(value = "buildingId", required = false) Long buildingId,
                              @RequestParam(value = "status", required = false) TicketStatus status,
                              Authentication auth,
                              Model model) {
        boolean isStaffScoper = isStaffScoped(auth);
        Long effectiveBuildingId = buildingId;
        List<Building> buildings;

        if (isStaffScoper && staffScope != null && staffScope.buildingId(auth).isPresent()) {
            effectiveBuildingId = staffScope.buildingId(auth).get();
            buildings = buildingRepository.findById(effectiveBuildingId).stream().toList();
        } else {
            buildings = buildingRepository.findAll();
        }

        List<MaintenanceTicket> tickets = ticketService.getTicketsForAdmin(effectiveBuildingId, status);

        model.addAttribute("buildings", buildings);
        model.addAttribute("selectedBuildingId", effectiveBuildingId);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("tickets", tickets);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("pageTitle", "Yêu cầu sửa chữa & Sự cố");
        model.addAttribute("pageSubtitle", "Quản lý và tiếp nhận các yêu cầu bảo trì, báo hỏng thiết bị từ sinh viên");
        model.addAttribute("activeMenu", "tickets");

        return "admin/tickets/list";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") TicketStatus status,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        ticketService.updateStatus(id, status, auth);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái ticket #" + id + " thành " + status + " thành công!");
        return "redirect:" + base();
    }

    private boolean isStaffScoped(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return false;
        }
        boolean isAll = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_QUAN_LY".equals(a.getAuthority()));
        return !isAll;
    }

    private String base() {
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes sra) {
                String uri = sra.getRequest().getRequestURI();
                if (uri != null && uri.startsWith("/manage")) {
                    return "/manage/tickets";
                }
            }
        } catch (Exception ignored) {}
        return "/admin/tickets";
    }
}
