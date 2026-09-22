package com.ktx.web.admin;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.RoomApplication;
import com.ktx.service.RegistrationPeriodService;
import com.ktx.service.RoomApplicationService;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Controller
@RequestMapping({"/manage/applications", "/admin/applications"})
@PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAuthority('application.read')")
public class AdminApplicationController {

    private final RoomApplicationService roomApplicationService;
    private final RegistrationPeriodService periodService;

    public AdminApplicationController(RoomApplicationService roomApplicationService,
                                      RegistrationPeriodService periodService) {
        this.roomApplicationService = roomApplicationService;
        this.periodService = periodService;
    }

    private String base() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null && attrs.getRequest() != null && attrs.getRequest().getRequestURI() != null) {
                return attrs.getRequest().getRequestURI().startsWith("/manage") ? "/manage/applications" : "/admin/applications";
            }
        } catch (Exception ignored) {
        }
        return "/admin/applications";
    }

    @GetMapping
    public String list(@RequestParam(value = "periodId", required = false) Long periodId, Model model) {
        List<RegistrationPeriod> periods = periodService.listAll();
        
        Long selectedPeriodId = periodId;
        if (selectedPeriodId == null && !periods.isEmpty()) {
            selectedPeriodId = periods.get(0).getId();
        }

        List<RoomApplication> applications = List.of();
        if (selectedPeriodId != null) {
            applications = roomApplicationService.listAllByPeriod(selectedPeriodId);
        }

        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", selectedPeriodId);
        model.addAttribute("applications", applications);

        page(model, "Danh sách đơn đăng ký", "Quản lý và duyệt đơn nguyện vọng phòng ở ký túc xá");
        return "admin/applications/list";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable("id") Long id,
                         @RequestParam(value = "periodId", required = false) Long periodId,
                         RedirectAttributes redirectAttributes) {
        try {
            roomApplicationService.rejectApplication(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn đăng ký thành công");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + base() + (periodId != null ? "?periodId=" + periodId : "");
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "applications");
    }
}
