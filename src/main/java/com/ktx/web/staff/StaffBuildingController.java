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

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'QUAN_LY', 'CAN_BO') or hasAuthority('room.read')")
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

    @GetMapping({"/manage/buildings/rooms", "/staff/buildings/rooms"})
    public String redirectRooms(Principal principal, HttpServletRequest request) {
        String base = request != null && request.getRequestURI().startsWith("/manage") ? "/manage" : "/staff";
        Staff staff = staffRepository.findByUserUsername(principal.getName())
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException(StaffScope.DENIED_STAFF));
        if (staff.getAssignedBuilding() == null) {
            throw new org.springframework.security.access.AccessDeniedException(StaffScope.DENIED_STAFF);
        }
        Long assignedBuildingId = staff.getAssignedBuilding().getId();
        return "redirect:" + base + "/buildings/" + assignedBuildingId + "/rooms";
    }

    @GetMapping({"/manage/buildings/{id}/rooms", "/staff/buildings/{id}/rooms"})
    public String viewRooms(@PathVariable Long id, Authentication authentication, HttpServletRequest request, Model model) {
        staffScope.assertBuilding(authentication, id);

        Building building = buildingService.getById(id);
        Map<Integer, List<RoomDiagramDto>> groupedRooms = roomService.getRoomDiagramGroupByFloor(id);

        model.addAttribute("building", building);
        model.addAttribute("floors", groupedRooms);
        model.addAttribute("pageTitle", "Sơ đồ phòng");
        model.addAttribute("pageSubtitle", "Tòa " + building.getCode() + " — Danh sách phòng và giường theo tầng");
        model.addAttribute("activeMenu", "rooms");
        model.addAttribute("baseUrl", request != null && request.getRequestURI().startsWith("/manage") ? "/manage" : "/staff");

        return "staff/buildings/rooms";
    }
}
