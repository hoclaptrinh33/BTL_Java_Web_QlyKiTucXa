package com.ktx.web.admin;

import java.util.List;

import jakarta.validation.Valid;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.User;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.domain.enums.PeriodType;
import com.ktx.dto.RegistrationPeriodForm;
import com.ktx.security.KtxUserDetails;
import com.ktx.service.RegistrationPeriodService;

@Controller
@RequestMapping({"/manage/periods", "/admin/periods"})
@PreAuthorize("hasAnyRole('ADMIN', 'QUAN_LY') or hasAuthority('period.manage')")
public class AdminPeriodController {

    private final RegistrationPeriodService periodService;
    private final com.ktx.repository.BuildingRepository buildingRepository;

    public AdminPeriodController(RegistrationPeriodService periodService,
                                 com.ktx.repository.BuildingRepository buildingRepository) {
        this.periodService = periodService;
        this.buildingRepository = buildingRepository;
    }

    private String base(HttpServletRequest request) {
        return (request != null && request.getRequestURI() != null && request.getRequestURI().startsWith("/admin"))
                ? "/admin/periods" : "/manage/periods";
    }

    @GetMapping
    public String list(Model model) {
        page(model, "Đợt đăng ký", "Quản lý vòng đời đợt nộp đơn ở KTX");
        List<RegistrationPeriod> periods = periodService.listAll();
        model.addAttribute("periods", periods);
        return "admin/periods/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        RegistrationPeriodForm form = new RegistrationPeriodForm();
        form.setStatus(PeriodStatus.DRAFT);
        form.setGenderScope(com.ktx.domain.enums.PeriodGenderScope.ALL);
        form.setMinConductScore(0);
        form.setDepositRatio(java.math.BigDecimal.valueOf(0.50));
        formModel(model, form, null);
        return "admin/periods/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") RegistrationPeriodForm form, BindingResult binding,
                         Authentication authentication, Model model, RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        if (binding.hasErrors()) {
            formModel(model, form, null);
            return "admin/periods/form";
        }
        try {
            KtxUserDetails userDetails = (KtxUserDetails) authentication.getPrincipal();
            User creator = userDetails.getUser();
            RegistrationPeriod created = periodService.create(form, creator);
            redirectAttributes.addFlashAttribute("successMessage", "Đã tạo đợt " + created.getName());
            return "redirect:" + base(request);
        } catch (BusinessException ex) {
            binding.reject(null, ex.getMessage());
            formModel(model, form, null);
            return "admin/periods/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
        try {
            RegistrationPeriod period = periodService.getByIdWithDetails(id);
            RegistrationPeriodForm form = new RegistrationPeriodForm();
            form.setName(period.getName());
            form.setPeriodType(period.getPeriodType());
            form.setAcademicYear(period.getAcademicYear());
            form.setOpenAt(period.getOpenAt());
            form.setCloseAt(period.getCloseAt());
            form.setTermStart(period.getTermStart());
            form.setTermEnd(period.getTermEnd());
            form.setStatus(period.getStatus());

            // Nâng cấp các trường
            form.setGenderScope(period.getGenderScope());
            form.setMinConductScore(period.getMinConductScore());
            form.setTargetCohort(period.getTargetCohort());
            form.setTargetQuota(period.getTargetQuota());
            form.setPaymentDeadline(period.getPaymentDeadline());
            form.setCheckinStart(period.getCheckinStart());
            form.setCheckinEnd(period.getCheckinEnd());
            form.setDepositRatio(period.getDepositRatio());
            form.setPaymentGuide(period.getPaymentGuide());
            form.setRequireDocumentProof(period.getRequireDocumentProof());
            form.setTermsAndConditions(period.getTermsAndConditions());
            form.setDescription(period.getDescription());
            form.setContactPhone(period.getContactPhone());
            form.setContactEmail(period.getContactEmail());

            if (period.getBuildings() != null) {
                form.setBuildingIds(period.getBuildings().stream().map(com.ktx.domain.Building::getId).toList());
            }

            formModel(model, form, id);
            return "admin/periods/form";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:" + base(request);
        }
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") RegistrationPeriodForm form,
                         BindingResult binding, Model model, RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        if (binding.hasErrors()) {
            formModel(model, form, id);
            return "admin/periods/form";
        }
        try {
            RegistrationPeriod updated = periodService.update(id, form);
            redirectAttributes.addFlashAttribute("successMessage", "Đã cập nhật đợt " + updated.getName());
            return "redirect:" + base(request);
        } catch (BusinessException ex) {
            binding.reject(null, ex.getMessage());
            formModel(model, form, id);
            return "admin/periods/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes,
                         HttpServletRequest request) {
        try {
            RegistrationPeriod period = periodService.getById(id);
            String name = period.getName();
            periodService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa đợt " + name);
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + base(request);
    }

    @PostMapping("/{id}/open")
    public String open(@PathVariable Long id, RedirectAttributes redirectAttributes,
                       HttpServletRequest request) {
        try {
            RegistrationPeriod period = periodService.transitionToOpen(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã mở đợt " + period.getName());
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + base(request);
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, RedirectAttributes redirectAttributes,
                        HttpServletRequest request) {
        try {
            RegistrationPeriod period = periodService.transitionToClose(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã đóng đợt " + period.getName());
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:" + base(request);
    }

    @ModelAttribute("periodTypes")
    public PeriodType[] periodTypes() {
        return PeriodType.values();
    }

    @ModelAttribute("periodStatuses")
    public PeriodStatus[] periodStatuses() {
        return PeriodStatus.values();
    }

    @ModelAttribute("genderScopes")
    public com.ktx.domain.enums.PeriodGenderScope[] genderScopes() {
        return com.ktx.domain.enums.PeriodGenderScope.values();
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "periods");
    }

    private void formModel(Model model, RegistrationPeriodForm form, Long periodId) {
        boolean editing = periodId != null;
        page(model, editing ? "Sửa đợt đăng ký" : "Thêm đợt đăng ký",
                editing ? "Cập nhật thông tin đợt đăng ký" : "Tạo mới một đợt đăng ký");
        model.addAttribute("form", form);
        model.addAttribute("periodId", periodId);
        model.addAttribute("allBuildings", buildingRepository.findByActiveTrueOrderByNameAsc());
    }
}
