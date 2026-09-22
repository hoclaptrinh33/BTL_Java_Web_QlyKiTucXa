package com.ktx.web.manage;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.ktx.domain.Building;
import com.ktx.dto.DashboardSnapshot;
import com.ktx.dto.DebtByMonthDto;
import com.ktx.repository.BuildingRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.DashboardService;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY', 'STAFF', 'CAN_BO') or hasAuthority('report.read') or hasAuthority('room.read')")
public class ManageDashboardController {

    private final DashboardService dashboardService;
    private final StaffScope staffScope;
    private final BuildingRepository buildingRepository;

    public ManageDashboardController(DashboardService dashboardService,
                                     StaffScope staffScope,
                                     BuildingRepository buildingRepository) {
        this.dashboardService = dashboardService;
        this.staffScope = staffScope;
        this.buildingRepository = buildingRepository;
    }

    @GetMapping({"/manage", "/manage/"})
    public String rootRedirect() {
        return "redirect:/manage/dashboard";
    }

    @GetMapping("/manage/dashboard")
    public String dashboard(Authentication auth, Model model) {
        Optional<Long> buildingIdOpt = staffScope != null ? staffScope.buildingId(auth) : Optional.empty();

        if (buildingIdOpt.isPresent()) {
            Long buildingId = buildingIdOpt.get();
            Building building = buildingRepository.findById(buildingId).orElse(null);
            DashboardSnapshot dash = dashboardService.loadForBuilding(buildingId);

            model.addAttribute("pageTitle", "Tổng quan");
            model.addAttribute("pageSubtitle", "Bảng điều khiển " + (building != null ? building.getName() : "tòa nhà"));
            model.addAttribute("activeMenu", "dashboard");
            model.addAttribute("dash", dash);
            model.addAttribute("building", building);
            return "staff/dashboard";
        }

        // Quản lý (Phạm vi ALL): Xem toàn bộ KPI vận hành KTX
        DashboardSnapshot dash = dashboardService.load();
        model.addAttribute("pageTitle", "Tổng quan vận hành");
        model.addAttribute("pageSubtitle", "Bảng điều khiển hoạt động lưu trú, phòng ở và dịch vụ KTX");
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("dash", dash);
        return "manage/dashboard";
    }

    @GetMapping("/manage/api/occupancy")
    @ResponseBody
    public Map<String, Object> occupancyChart() {
        return dashboardService.getOccupancyChartData();
    }

    @GetMapping("/manage/api/debt-by-month")
    @ResponseBody
    public DebtByMonthDto debtChart() {
        return dashboardService.calculateDebtByMonth();
    }
}
