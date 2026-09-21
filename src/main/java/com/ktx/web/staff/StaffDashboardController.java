package com.ktx.web.staff;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ktx.domain.Building;
import com.ktx.repository.BuildingRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.DashboardService;

@Controller
public class StaffDashboardController {

    private final DashboardService dashboardService;
    private final StaffScope staffScope;
    private final BuildingRepository buildingRepository;

    public StaffDashboardController(DashboardService dashboardService,
                                    StaffScope staffScope,
                                    BuildingRepository buildingRepository) {
        this.dashboardService = dashboardService;
        this.staffScope = staffScope;
        this.buildingRepository = buildingRepository;
    }

    @GetMapping("/staff/dashboard")
    public String dashboard(Authentication auth,
                            @RequestParam(value = "buildingId", required = false) Long requestedBuildingId,
                            Model model) {
        Long targetBuildingId = staffScope.buildingId(auth).orElseGet(() -> {
            if (requestedBuildingId != null) {
                return requestedBuildingId;
            }
            return buildingRepository.findAll().stream()
                    .map(Building::getId)
                    .findFirst()
                    .orElse(null);
        });

        if (targetBuildingId == null) {
            model.addAttribute("pageTitle", "Tổng quan tòa");
            model.addAttribute("pageSubtitle", "Chưa có dữ liệu tòa nhà");
            model.addAttribute("activeMenu", "dashboard");
            model.addAttribute("dash", null);
            return "staff/dashboard";
        }

        staffScope.assertBuilding(auth, targetBuildingId);
        Building building = buildingRepository.findById(targetBuildingId).orElse(null);

        model.addAttribute("pageTitle", "Tổng quan");
        model.addAttribute("pageSubtitle", "Bảng điều khiển " + (building != null ? building.getName() : "tòa nhà"));
        model.addAttribute("activeMenu", "dashboard");
        model.addAttribute("dash", dashboardService.loadForBuilding(targetBuildingId));
        model.addAttribute("building", building);
        return "staff/dashboard";
    }
}
