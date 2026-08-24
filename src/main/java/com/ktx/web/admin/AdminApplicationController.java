package com.ktx.web.admin;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import com.ktx.repository.RegistrationPeriodRepository;
import com.ktx.service.RoomApplicationService;

@Controller
@RequestMapping("/admin/applications")
public class AdminApplicationController {

    private final RoomApplicationService roomApplicationService;
    private final RegistrationPeriodRepository periodRepository;

    @Autowired
    public AdminApplicationController(RoomApplicationService roomApplicationService,
                                      RegistrationPeriodRepository periodRepository) {
        this.roomApplicationService = roomApplicationService;
        this.periodRepository = periodRepository;
    }

    @GetMapping
    public String list(@RequestParam(value = "periodId", required = false) Long periodId, Model model) {
        List<RegistrationPeriod> periods = periodRepository.findAllWithCreator();
        
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
    public String reject(@PathVariable("id") Long id, @RequestParam("periodId") Long periodId, RedirectAttributes redirectAttributes) {
        try {
            roomApplicationService.rejectApplication(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã từ chối đơn đăng ký thành công");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/applications?periodId=" + periodId;
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "applications");
    }
}
