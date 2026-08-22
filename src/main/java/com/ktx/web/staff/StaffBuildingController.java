package com.ktx.web.staff;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.ktx.domain.Building;
import com.ktx.domain.Staff;
import com.ktx.dto.RoomDiagramDto;
import com.ktx.repository.StaffRepository;
import com.ktx.security.StaffScope;
import com.ktx.service.BuildingService;
import com.ktx.service.RoomService;

@Controller
public class StaffBuildingController {

    private final RoomService roomService;
    private final BuildingService buildingService;
    private final StaffRepository staffRepository;
    private final StaffScope staffScope;

    public StaffBuildingController(RoomService roomService, BuildingService buildingService,
            StaffRepository staffRepository, StaffScope staffScope) {
        this.roomService = roomService;
        this.buildingService = buildingService;
        this.staffRepository = staffRepository;
        this.staffScope = staffScope;
    }

    @GetMapping("/staff/buildings/rooms")
    public String redirectRooms(Principal principal) {
        Staff staff = staffRepository.findByUserUsername(principal.getName())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Không tìm thấy thông tin cán bộ"));
        Long assignedBuildingId = staff.getAssignedBuilding().getId();
        return "redirect:/staff/buildings/" + assignedBuildingId + "/rooms";
    }

    @GetMapping("/staff/buildings/{id}/rooms")
    public String viewRooms(@PathVariable Long id, Authentication authentication, Model model) {
        staffScope.assertBuilding(authentication, id);

        Building building = buildingService.getById(id);
        Map<Integer, List<RoomDiagramDto>> groupedRooms = roomService.getRoomDiagramGroupByFloor(id);

        model.addAttribute("building", building);
        model.addAttribute("floors", groupedRooms);
        model.addAttribute("pageTitle", "Sơ đồ phòng");
        model.addAttribute("pageSubtitle", "Tòa " + building.getCode() + " — Danh sách phòng và giường theo tầng");
        model.addAttribute("activeMenu", "rooms");

        return "staff/buildings/rooms";
    }
}
