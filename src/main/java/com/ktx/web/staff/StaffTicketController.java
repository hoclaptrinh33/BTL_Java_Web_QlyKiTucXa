package com.ktx.web.staff;

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

import com.ktx.domain.MaintenanceTicket;
import com.ktx.domain.enums.TicketStatus;
import com.ktx.service.TicketService;

@Controller
@RequestMapping("/staff/tickets")
public class StaffTicketController {

    private final TicketService ticketService;

    public StaffTicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public String listTickets(@RequestParam(value = "status", required = false) TicketStatus status,
                              Authentication auth,
                              Model model) {
        List<MaintenanceTicket> tickets = ticketService.getTicketsForStaff(auth, status);

        model.addAttribute("tickets", tickets);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("pageTitle", "Sự cố & Báo hỏng thiết bị");
        model.addAttribute("pageSubtitle", "Theo dõi và cập nhật tiến độ xử lý ticket của tòa phụ trách");
        model.addAttribute("activeMenu", "tickets");

        return "staff/tickets/list";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable("id") Long id,
                               @RequestParam("status") TicketStatus status,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        ticketService.updateStatus(id, status, auth);
        redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái ticket #" + id + " thành " + status + " thành công!");
        return "redirect:/staff/tickets";
    }
}
