package com.ktx.web.admin;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.Room;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.RoomForm;
import com.ktx.service.BuildingService;
import com.ktx.service.RoomService;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;
    private final BuildingService buildingService;

    public AdminRoomController(RoomService roomService, BuildingService buildingService) {
        this.roomService = roomService;
        this.buildingService = buildingService;
    }

    @GetMapping
    public String list(@RequestParam(name = "buildingId", required = false) Long buildingId, Model model) {
        page(model, "Phòng ở", "Danh mục phòng theo tòa — giường tự tạo theo loại");
        model.addAttribute("rooms", roomService.listRows(buildingId));
        model.addAttribute("buildings", buildingService.listAll());
        model.addAttribute("buildingId", buildingId);
        return "admin/rooms/list";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(name = "buildingId", required = false) Long buildingId, Model model) {
        RoomForm form = new RoomForm();
        form.setStatus(RoomStatus.ACTIVE);
        form.setFloor(1);
        form.setRoomType(RoomType.STANDARD_6);
        form.setPricePerTerm(RoomService.defaultPrice(RoomType.STANDARD_6));
        if (buildingId != null) {
            form.setBuildingId(buildingId);
        }
        formModel(model, form, null, false);
        return "admin/rooms/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") RoomForm form, BindingResult binding,
            Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            formModel(model, form, null, false);
            return "admin/rooms/form";
        }
        try {
            Room created = roomService.create(form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã tạo phòng " + created.getBuilding().getCode() + "-" + created.getRoomNumber()
                            + " · " + created.getCapacity() + " giường");
            return "redirect:/admin/rooms?buildingId=" + created.getBuilding().getId();
        } catch (DuplicateFieldException ex) {
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            formModel(model, form, null, false);
            return "admin/rooms/form";
        } catch (BusinessException ex) {
            binding.reject("business", ex.getMessage());
            formModel(model, form, null, false);
            return "admin/rooms/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Room room = roomService.getById(id);
            RoomForm form = new RoomForm();
            form.setBuildingId(room.getBuilding().getId());
            form.setRoomNumber(room.getRoomNumber());
            form.setFloor(room.getFloor());
            form.setRoomType(room.getRoomType());
            form.setPricePerTerm(room.getPricePerTerm());
            form.setStatus(room.getStatus());
            formModel(model, form, id, true);
            return "admin/rooms/form";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/rooms";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") RoomForm form,
            BindingResult binding, Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            formModel(model, form, id, true);
            return "admin/rooms/form";
        }
        try {
            Room updated = roomService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã cập nhật phòng " + updated.getBuilding().getCode() + "-" + updated.getRoomNumber());
            return "redirect:/admin/rooms?buildingId=" + updated.getBuilding().getId();
        } catch (DuplicateFieldException ex) {
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            formModel(model, form, id, true);
            return "admin/rooms/form";
        } catch (BusinessException ex) {
            if (RoomService.NOT_FOUND.equals(ex.getMessage())) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/admin/rooms";
            }
            binding.reject("business", ex.getMessage());
            formModel(model, form, id, true);
            return "admin/rooms/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Room room = roomService.getById(id);
            Long buildingId = room.getBuilding().getId();
            roomService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa phòng " + room.getRoomNumber());
            return "redirect:/admin/rooms?buildingId=" + buildingId;
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/rooms";
        }
    }

    @ModelAttribute("roomTypes")
    public RoomType[] roomTypes() {
        return RoomType.values();
    }

    @ModelAttribute("roomStatuses")
    public RoomStatus[] roomStatuses() {
        return RoomStatus.values();
    }

    private void formModel(Model model, RoomForm form, Long roomId, boolean editing) {
        page(model, editing ? "Sửa phòng" : "Thêm phòng",
                editing ? "Cập nhật giá, tầng, trạng thái" : "Tạo phòng — hệ thống tự sinh giường");
        model.addAttribute("form", form);
        model.addAttribute("roomId", roomId);
        model.addAttribute("editing", editing);
        var buildings = buildingService.listAll();
        model.addAttribute("buildings", buildings);
        model.addAttribute("buildingOptions", buildings.stream().map(b -> Map.of(
                "id", b.getId(),
                "code", b.getCode(),
                "name", b.getName(),
                "genderPolicy", b.getGenderPolicy().name())).toList());
        model.addAttribute("capacity", roomService.capacityOf(form.getRoomType()));
        Map<String, BigDecimal> defaultPrices = new LinkedHashMap<>();
        Map<String, Integer> capacities = new LinkedHashMap<>();
        for (RoomType type : RoomType.values()) {
            defaultPrices.put(type.name(), RoomService.defaultPrice(type));
            capacities.put(type.name(), roomService.capacityOf(type));
        }
        model.addAttribute("defaultPrices", defaultPrices);
        model.addAttribute("capacities", capacities);
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "rooms");
    }
}
