package com.ktx.web.admin;

import java.util.List;

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
import com.ktx.service.TicketService;

@Controller
@RequestMapping("/admin/tickets")
public class AdminTicketController {

    private final TicketService ticketService;
    private final BuildingRepository buildingRepository;

    public AdminTicketController(TicketService ticketService, BuildingRepository buildingRepository) {
        this.ticketService = ticketService;
        this.buildingRepository = buildingRepository;
    }

    @GetMapping
    public String listTickets(@RequestParam(value = "buildingId", required = false) Long buildingId,
                              @RequestParam(value = "status", required = false) TicketStatus status,
                              Model model) {
        List<Building> buildings = buildingRepository.findAll();
        List<MaintenanceTicket> tickets = ticketService.getTicketsForAdmin(buildingId, status);

        model.addAttribute("buildings", buildings);
        model.addAttribute("selectedBuildingId", buildingId);
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
        return "redirect:/admin/tickets";
    }
}
