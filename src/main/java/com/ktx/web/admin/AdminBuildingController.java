package com.ktx.web.admin;

import java.util.List;

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
import com.ktx.domain.Building;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.dto.BuildingForm;
import com.ktx.dto.BuildingOverviewDto;
import com.ktx.dto.BuildingRowDto;
import com.ktx.service.BuildingService;

@Controller
@RequestMapping("/admin/buildings")
public class AdminBuildingController {

    private final BuildingService buildingService;

    public AdminBuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q,
                       @RequestParam(name = "gender", required = false) String gender,
                       @RequestParam(name = "active", required = false) Boolean active,
                       Model model) {
        page(model, "Quản lý tòa nhà", "Danh mục tòa Nam / Nữ và phòng ở");

        BuildingOverviewDto overview = buildingService.loadOverviewStats();
        if (overview == null) {
            overview = new BuildingOverviewDto();
        }
        List<BuildingRowDto> buildingRows = buildingService.listBuildingRows(q, gender, active);
        List<Building> rawBuildings = buildingService.listAll();

        if ((buildingRows == null || buildingRows.isEmpty()) && rawBuildings != null && !rawBuildings.isEmpty()) {
            buildingRows = rawBuildings.stream().map(b -> {
                BuildingRowDto row = new BuildingRowDto();
                row.setId(b.getId());
                row.setCode(b.getCode());
                row.setName(b.getName());
                row.setGenderPolicy(b.getGenderPolicy());
                row.setGenderLabel(b.getGenderPolicy() == BuildingGenderPolicy.MALE ? "Nam" : "Nữ");
                row.setActive(Boolean.TRUE.equals(b.getActive()));
                row.setStatusLabel(Boolean.TRUE.equals(b.getActive()) ? "Hoạt động" : "Tạm tắt");
                row.setStatusClass(Boolean.TRUE.equals(b.getActive()) ? "is-active" : "is-inactive");
                row.setZone("Khu " + b.getCode());
                row.setZoneClass("is-zone-a");
                row.setFloorCount(1);
                row.setRoomCount(0);
                row.setBedCount(0);
                row.setOccupiedBeds(0);
                row.setVacantBeds(0);
                row.setOccupancyPercent(0.0);
                row.setAddress("Địa chỉ: Khu " + b.getCode() + ", Trường ĐH XYZ");
                row.setImageUrl("/images/buildings/building-1.jpg");
                return row;
            }).toList();
        }

        model.addAttribute("overview", overview);
        model.addAttribute("buildingRows", buildingRows);
        model.addAttribute("buildings", rawBuildings);
        model.addAttribute("q", q != null ? q : "");
        model.addAttribute("genderFilter", gender != null ? gender : "ALL");
        model.addAttribute("activeFilter", active);

        return "admin/buildings/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        BuildingForm form = new BuildingForm();
        form.setActive(true);
        formModel(model, form, null, false);
        return "admin/buildings/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BuildingForm form, BindingResult binding,
            Model model, RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            formModel(model, form, null, false);
            return "admin/buildings/form";
        }
        try {
            Building created = buildingService.create(form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo tòa " + created.getCode());
            return "redirect:/admin/buildings";
        } catch (DuplicateFieldException ex) {
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            formModel(model, form, null, false);
            return "admin/buildings/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Building building = buildingService.getById(id);
            BuildingForm form = new BuildingForm();
            form.setCode(building.getCode());
            form.setName(building.getName());
            form.setGenderPolicy(building.getGenderPolicy());
            form.setActive(Boolean.TRUE.equals(building.getActive()));
            formModel(model, form, id, buildingService.hasOccupyingContracts(id));
            return "admin/buildings/form";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/buildings";
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") BuildingForm form,
            BindingResult binding, Model model, RedirectAttributes redirectAttributes) {
        boolean occupying = buildingService.hasOccupyingContracts(id);
        if (binding.hasErrors()) {
            formModel(model, form, id, occupying);
            return "admin/buildings/form";
        }
        try {
            Building updated = buildingService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật tòa " + updated.getCode());
            return "redirect:/admin/buildings";
        } catch (DuplicateFieldException ex) {
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            formModel(model, form, id, occupying);
            return "admin/buildings/form";
        } catch (BusinessException ex) {
            if (BuildingService.NOT_FOUND.equals(ex.getMessage())) {
                redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
                return "redirect:/admin/buildings";
            }
            binding.rejectValue("genderPolicy", "locked", ex.getMessage());
            formModel(model, form, id, occupying);
            return "admin/buildings/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Building building = buildingService.getById(id);
            String code = building.getCode();
            buildingService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa tòa " + code);
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/buildings";
    }

    @ModelAttribute("genderPolicies")
    public BuildingGenderPolicy[] genderPolicies() {
        return BuildingGenderPolicy.values();
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "buildings");
    }

    private static void formModel(Model model, BuildingForm form, Long buildingId, boolean occupying) {
        boolean editing = buildingId != null;
        page(model, editing ? "Sửa tòa" : "Thêm tòa",
                editing ? "Cập nhật thông tin tòa" : "Tạo tòa Nam hoặc Nữ");
        model.addAttribute("form", form);
        model.addAttribute("buildingId", buildingId);
        model.addAttribute("occupying", occupying);
    }
}
