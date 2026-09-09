package com.ktx.web.admin;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.AllocationItem;
import com.ktx.domain.AllocationRun;
import com.ktx.domain.Contract;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.enums.AllocationResult;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.service.AllocationService;
import com.ktx.service.ContractService;

@Controller
@RequestMapping("/admin/allocations")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAllocationController {

    private final AllocationService allocationService;
    private final RoomApplicationRepository roomApplicationRepository;
    private final ContractService contractService;

    public AdminAllocationController(AllocationService allocationService,
                                     RoomApplicationRepository roomApplicationRepository,
                                     ContractService contractService) {
        this.allocationService = allocationService;
        this.roomApplicationRepository = roomApplicationRepository;
        this.contractService = contractService;
    }

    @GetMapping
    public String index(@RequestParam(value = "periodId", required = false) Long periodId, Model model) {
        List<RegistrationPeriod> periods = allocationService.getAvailablePeriods();
        Long selectedPeriodId = periodId;
        if (selectedPeriodId == null && !periods.isEmpty()) {
            selectedPeriodId = periods.stream()
                    .filter(p -> p.getStatus() == PeriodStatus.CLOSED || p.getStatus() == PeriodStatus.ALLOCATING)
                    .map(RegistrationPeriod::getId)
                    .findFirst()
                    .orElse(periods.get(0).getId());
        }

        RegistrationPeriod selectedPeriod = null;
        List<AllocationRun> runs = List.of();
        long submittedCount = 0;

        if (selectedPeriodId != null) {
            final Long pid = selectedPeriodId;
            selectedPeriod = periods.stream().filter(p -> p.getId().equals(pid)).findFirst().orElse(null);
            runs = allocationService.getRunsByPeriod(selectedPeriodId);
            submittedCount = roomApplicationRepository.findByPeriodIdAndStatus(selectedPeriodId, ApplicationStatus.SUBMITTED).size();
        }

        model.addAttribute("periods", periods);
        model.addAttribute("selectedPeriodId", selectedPeriodId);
        model.addAttribute("selectedPeriod", selectedPeriod);
        model.addAttribute("runs", runs);
        model.addAttribute("submittedCount", submittedCount);

        page(model, "Phân bổ chỗ ở", "Xem trước (Dry-run) và quản lý phân bổ chỗ ở theo điểm ưu tiên");
        return "admin/allocations/index";
    }

    @GetMapping("/periods/{id}")
    public String periodDetail(@PathVariable("id") Long id) {
        return "redirect:/admin/allocations?periodId=" + id;
    }

    @PostMapping("/periods/{id}/preview")
    public String preview(@PathVariable("id") Long id,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            Long adminUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof KtxUserDetails userDetails) {
                adminUserId = userDetails.getUser().getId();
            }

            AllocationRun run = allocationService.previewAndStore(id, adminUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chạy xem trước phân bổ (Dry-run) thành công. Dữ liệu giường và đơn đăng ký chưa bị thay đổi.");
            return "redirect:/admin/allocations/runs/" + run.getId();
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/allocations?periodId=" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi chạy phân bổ: " + ex.getMessage());
            return "redirect:/admin/allocations?periodId=" + id;
        }
    }

    @PostMapping("/periods/{id}/commit")
    public String commit(@PathVariable("id") Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            Long adminUserId = null;
            if (authentication != null && authentication.getPrincipal() instanceof KtxUserDetails userDetails) {
                adminUserId = userDetails.getUser().getId();
            }

            AllocationRun run = allocationService.commit(id, adminUserId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Đã chốt phân bổ thành công (Lượt #" + run.getId() + "). Toàn bộ giường đã được khóa và tạo hợp đồng nháp (DRAFT).");
            return "redirect:/admin/allocations/runs/" + run.getId();
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/allocations?periodId=" + id;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi chốt phân bổ: " + ex.getMessage());
            return "redirect:/admin/allocations?periodId=" + id;
        }
    }

    @PostMapping("/contracts/{contractId}/cancel-draft")
    public String cancelDraft(@PathVariable("contractId") Long contractId,
                              @RequestParam(value = "runId", required = false) Long runId,
                              RedirectAttributes redirectAttributes) {
        try {
            contractService.cancelDraft(contractId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy hợp đồng nháp và nhả giường thành công.");
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi hủy hợp đồng nháp: " + ex.getMessage());
        }

        if (runId != null) {
            return "redirect:/admin/allocations/runs/" + runId;
        }
        return "redirect:/admin/allocations";
    }

    @PostMapping("/assign-manual")
    public String assignManual(@RequestParam("studentId") Long studentId,
                               @RequestParam("bedId") Long bedId,
                               @RequestParam(value = "periodId", required = false) Long periodId,
                               @RequestParam(value = "note", required = false) String note,
                               RedirectAttributes redirectAttributes) {
        try {
            Contract contract = allocationService.assignManual(studentId, bedId, periodId, note);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Gán tay thành công! Đã tạo hợp đồng nháp " + contract.getContractNo() + " cho sinh viên " + contract.getStudent().getFullName());
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi gán tay: " + ex.getMessage());
        }

        if (periodId != null) {
            return "redirect:/admin/allocations?periodId=" + periodId;
        }
        return "redirect:/admin/allocations";
    }

    @GetMapping("/runs/{runId}")
    public String runDetail(@PathVariable("runId") Long runId, Model model, RedirectAttributes redirectAttributes) {
        try {
            AllocationRun run = allocationService.getRun(runId);
            List<AllocationItem> items = allocationService.getRunItems(runId);

            long assignedCount = items.stream().filter(i -> i.getResult() == AllocationResult.ASSIGNED).count();
            long waitlistCount = items.stream().filter(i -> i.getResult() == AllocationResult.WAITLISTED).count();
            long skippedCount = items.stream().filter(i -> i.getResult() == AllocationResult.SKIPPED).count();

            model.addAttribute("run", run);
            model.addAttribute("period", run.getPeriod());
            model.addAttribute("items", items);
            model.addAttribute("totalCount", items.size());
            model.addAttribute("assignedCount", assignedCount);
            model.addAttribute("waitlistCount", waitlistCount);
            model.addAttribute("skippedCount", skippedCount);

            page(model, "Kết quả xem trước phân bổ", "Bảng xếp hạng và giường dự kiến (Lượt chạy #" + run.getId() + ")");
            return "admin/allocations/run_detail";
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/allocations";
        }
    }

    @PostMapping("/runs/{runId}/discard")
    public String discardRun(@PathVariable("runId") Long runId, RedirectAttributes redirectAttributes) {
        try {
            AllocationRun run = allocationService.getRun(runId);
            Long periodId = run.getPeriod().getId();
            allocationService.discardRun(runId);
            redirectAttributes.addFlashAttribute("successMessage", "Đã hủy kết quả xem trước phân bổ #" + runId);
            return "redirect:/admin/allocations?periodId=" + periodId;
        } catch (BusinessException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/allocations";
        }
    }

    private static void page(Model model, String title, String subtitle) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("pageSubtitle", subtitle);
        model.addAttribute("activeMenu", "allocations");
    }
}
