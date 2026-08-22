package com.ktx.web.admin;

import jakarta.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.domain.Building;
import com.ktx.domain.enums.BuildingGenderPolicy;
import com.ktx.dto.BuildingForm;
import com.ktx.service.BuildingService;

@Controller
@RequestMapping("/admin/buildings")
public class AdminBuildingController {

    private final BuildingService buildingService;

    public AdminBuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    public String list(Model model) {
        page(model, "Tòa nhà", "Quản lý tòa Nam / Nữ");
        model.addAttribute("buildings", buildingService.listAll());
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
