package com.ktx.web.admin;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
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
import com.ktx.domain.Bed;
import com.ktx.domain.Room;
import com.ktx.domain.RoomAsset;
import com.ktx.domain.enums.AssetCategory;
import com.ktx.domain.enums.AssetCondition;
import com.ktx.domain.enums.BedStatus;
import com.ktx.domain.enums.RoomStatus;
import com.ktx.domain.enums.RoomType;
import com.ktx.dto.RoomAssetForm;
import com.ktx.dto.RoomBatchForm;
import com.ktx.dto.RoomBatchResult;
import com.ktx.dto.RoomForm;
import com.ktx.repository.BedRepository;
import com.ktx.service.AssetService;
import com.ktx.service.BedService;
import com.ktx.service.BuildingService;
import com.ktx.service.RoomService;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;
    private final BuildingService buildingService;
    private final BedService bedService;
    private final AssetService assetService;
    private final BedRepository bedRepository;

    public AdminRoomController(RoomService roomService, BuildingService buildingService,
            BedService bedService, AssetService assetService, BedRepository bedRepository) {
        this.roomService = roomService;
        this.buildingService = buildingService;
        this.bedService = bedService;
        this.assetService = assetService;
        this.bedRepository = bedRepository;
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

    @GetMapping("/batch")
    public String batchForm(@RequestParam(name = "buildingId", required = false) Long buildingId, Model model) {
        RoomBatchForm form = new RoomBatchForm();
        form.setStatus(RoomStatus.ACTIVE);
        form.setFloorFrom(1);
        form.setFloorTo(5);
        form.setRoomsPerFloor(10);
        form.setRoomType(RoomType.STANDARD_6);
        form.setPricePerTerm(RoomService.defaultPrice(RoomType.STANDARD_6));
        if (buildingId != null) {
            form.setBuildingId(buildingId);
        }
        batchModel(model, form);
        return "admin/rooms/batch";
    }

    @PostMapping("/batch")
    public String createBatch(@Valid @ModelAttribute("form") RoomBatchForm form, BindingResult binding,
            Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            batchModel(model, form);
            return "admin/rooms/batch";
        }
        try {
            RoomBatchResult result = roomService.createBatch(form);
            redirectAttributes.addFlashAttribute("successMessage", batchSuccessMessage(result));
            return "redirect:/admin/rooms?buildingId=" + result.getBuildingId();
        } catch (BusinessException ex) {
            if (RoomService.BATCH_EMPTY.equals(ex.getMessage())) {
                binding.reject("business", ex.getMessage());
            } else if (RoomService.FLOOR_RANGE_INVALID.equals(ex.getMessage())) {
                binding.rejectValue("floorTo", "range", ex.getMessage());
            } else {
                binding.reject("business", ex.getMessage());
            }
            batchModel(model, form);
            return "admin/rooms/batch";
        }
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

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id,
            @RequestParam(name = "assetId", required = false) Long assetId,
            Model model, RedirectAttributes redirectAttributes) {
        try {
            RoomAssetForm form = new RoomAssetForm();
            form.setQuantity(1);
            form.setCondition(AssetCondition.GOOD);
            form.setCategory(AssetCategory.FAN);
            if (assetId != null) {
                RoomAsset asset = assetService.getById(id, assetId);
                form.setName(asset.getName());
                form.setCategory(asset.getCategory());
                form.setQuantity(asset.getQuantity());
                form.setCondition(asset.getCondition());
                form.setNote(asset.getNote());
                form.setSerialNumber(asset.getSerialNumber());
            }
            detailModel(model, id, form, assetId);
            return "admin/rooms/detail";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/rooms";
        }
    }

    @GetMapping("/{id}/assets")
    public String assets(@PathVariable Long id) {
        return "redirect:/admin/rooms/" + id;
    }

    @PostMapping("/{id}/beds/{bedId}/status")
    public String updateBedStatus(@PathVariable Long id, @PathVariable Long bedId,
            @RequestParam("status") BedStatus status,
            @RequestParam("version") Long version,
            RedirectAttributes redirectAttributes) {
        try {
            Bed updated = bedService.updateStatus(id, bedId, status, version);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Giường " + updated.getBedCode() + " → " + BedService.statusLabel(updated.getStatus()));
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/rooms/" + id;
    }

    @PostMapping("/{id}/assets")
    public String createAsset(@PathVariable Long id, @Valid @ModelAttribute("assetForm") RoomAssetForm form,
            BindingResult binding, Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            try {
                detailModel(model, id, form, null);
                return "admin/rooms/detail";
            } catch (BusinessException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/admin/rooms";
            }
        }
        try {
            RoomAsset created = assetService.create(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm " + created.getName());
            return "redirect:/admin/rooms/" + id;
        } catch (BusinessException ex) {
            if (RoomService.NOT_FOUND.equals(ex.getMessage())) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/admin/rooms";
            }
            binding.reject("business", ex.getMessage());
            detailModel(model, id, form, null);
            return "admin/rooms/detail";
        }
    }

    @PostMapping("/{id}/assets/{assetId}/edit")
    public String updateAsset(@PathVariable Long id, @PathVariable Long assetId,
            @Valid @ModelAttribute("assetForm") RoomAssetForm form, BindingResult binding,
            Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            try {
                detailModel(model, id, form, assetId);
                return "admin/rooms/detail";
            } catch (BusinessException ex) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/admin/rooms/" + id;
            }
        }
        try {
            RoomAsset updated = assetService.update(id, assetId, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật " + updated.getName());
            return "redirect:/admin/rooms/" + id;
        } catch (BusinessException ex) {
            if (AssetService.NOT_FOUND.equals(ex.getMessage()) || RoomService.NOT_FOUND.equals(ex.getMessage())) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/admin/rooms/" + id;
            }
            binding.reject("business", ex.getMessage());
            detailModel(model, id, form, assetId);
            return "admin/rooms/detail";
        }
    }

    @PostMapping("/{id}/assets/{assetId}/delete")
    public String deleteAsset(@PathVariable Long id, @PathVariable Long assetId,
            RedirectAttributes redirectAttributes) {
        try {
            assetService.delete(id, assetId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tài sản");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/rooms/" + id;
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

    @ModelAttribute("assetCategories")
    public AssetCategory[] assetCategories() {
        return AssetCategory.values();
    }

    @ModelAttribute("assetConditions")
    public AssetCondition[] assetConditions() {
        return AssetCondition.values();
    }

    private void batchModel(Model model, RoomBatchForm form) {
        page(model, "Thêm phòng hàng loạt", "Sinh dải tầng — hệ thống tự tạo giường G1…Gn");
        model.addAttribute("form", form);
        catalog(model, form.getRoomType());
    }

    private void formModel(Model model, RoomForm form, Long roomId, boolean editing) {
        page(model, editing ? "Sửa phòng" : "Thêm phòng",
                editing ? "Cập nhật giá, tầng, trạng thái" : "Tạo phòng — hệ thống tự sinh giường");
        model.addAttribute("form", form);
        model.addAttribute("roomId", roomId);
        model.addAttribute("editing", editing);
        catalog(model, form.getRoomType());
    }

    private void catalog(Model model, RoomType selectedType) {
        var buildings = buildingService.listAll();
        model.addAttribute("buildings", buildings);
        model.addAttribute("buildingOptions", buildings.stream().map(b -> Map.of(
                "id", b.getId(),
                "code", b.getCode(),
                "name", b.getName(),
                "genderPolicy", b.getGenderPolicy().name())).toList());
        model.addAttribute("capacity", roomService.capacityOf(selectedType));
        Map<String, BigDecimal> defaultPrices = new LinkedHashMap<>();
        Map<String, Integer> capacities = new LinkedHashMap<>();
        for (RoomType type : RoomType.values()) {
            defaultPrices.put(type.name(), RoomService.defaultPrice(type));
            capacities.put(type.name(), roomService.capacityOf(type));
        }
        model.addAttribute("defaultPrices", defaultPrices);
        model.addAttribute("capacities", capacities);
    }

    static String batchSuccessMessage(RoomBatchResult result) {
        StringBuilder message = new StringBuilder("Đã tạo ")
                .append(result.getCreated())
                .append(" phòng · ")
                .append(result.getBedsCreated())
                .append(" giường");
        if (result.getFirstDoorCode() != null && result.getLastDoorCode() != null) {
            message.append(" (")
                    .append(result.getFirstDoorCode())
                    .append(" → ")
                    .append(result.getLastDoorCode())
                    .append(')');
        }
        if (result.getSkipped() > 0) {
            message.append(". Bỏ qua ")
                    .append(result.getSkipped())
                    .append(" số đã có");
            List<String> skipped = result.getSkippedNumbers();
            if (!skipped.isEmpty()) {
                int shown = Math.min(8, skipped.size());
                message.append(": ").append(String.join(", ", skipped.subList(0, shown)));
                if (skipped.size() > shown) {
                    message.append("…");
                }
            }
        }
        return message.toString();
    }

    private void detailModel(Model model, Long roomId, RoomAssetForm form, Long editAssetId) {
        Room room = roomService.getById(roomId);
        List<Bed> beds = bedRepository.findByRoomIdOrderByBedCodeAsc(roomId);
        long[] counts = new long[3];
        for (Bed bed : beds) {
            if (bed.getStatus() == BedStatus.OCCUPIED) {
                counts[0]++;
            } else if (bed.getStatus() == BedStatus.VACANT) {
                counts[1]++;
            } else {
                counts[2]++;
            }
        }
        page(model, "Phòng " + room.getBuilding().getCode() + "-" + room.getRoomNumber(),
                "Giường và tài sản — công tơ điện/nước mỗi phòng một cái");
        model.addAttribute("room", room);
        model.addAttribute("row", RoomService.toRow(room, counts));
        model.addAttribute("beds", beds);
        model.addAttribute("assets", assetService.list(roomId));
        model.addAttribute("assetForm", form);
        model.addAttribute("editAssetId", editAssetId);
        model.addAttribute("editingAsset", editAssetId != null);
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "rooms");
    }
}
