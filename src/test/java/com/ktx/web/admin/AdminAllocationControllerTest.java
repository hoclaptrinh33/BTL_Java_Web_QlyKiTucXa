package com.ktx.web.admin;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.ktx.domain.AllocationItem;
import com.ktx.domain.AllocationRun;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.User;
import com.ktx.domain.enums.AllocationResult;
import com.ktx.domain.enums.AllocationRunStatus;
import com.ktx.domain.enums.ApplicationStatus;
import com.ktx.domain.enums.PeriodStatus;
import com.ktx.repository.RoomApplicationRepository;
import com.ktx.security.KtxUserDetails;
import com.ktx.service.AllocationService;
import com.ktx.service.ContractService;

@ExtendWith(MockitoExtension.class)
class AdminAllocationControllerTest {

    @Mock
    private AllocationService allocationService;

    @Mock
    private RoomApplicationRepository roomApplicationRepository;

    @Mock
    private ContractService contractService;

    @Mock
    private Authentication authentication;

    @Mock
    private KtxUserDetails userDetails;

    private AdminAllocationController controller;

    private RegistrationPeriod period1;
    private User adminUser;

    @BeforeEach
    void setUp() {
        controller = new AdminAllocationController(allocationService, roomApplicationRepository, contractService);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");

        period1 = new RegistrationPeriod();
        period1.setId(10L);
        period1.setName("Đợt 1");
        period1.setStatus(PeriodStatus.CLOSED);
    }

    @Test
    @DisplayName("index() tải danh sách periods, chọn period CLOSED hoặc đầu tiên, và nạp lịch sử runs")
    void index_defaultSelection() {
        when(allocationService.getAvailablePeriods()).thenReturn(List.of(period1));
        when(allocationService.getRunsByPeriod(10L)).thenReturn(Collections.emptyList());
        when(roomApplicationRepository.findByPeriodIdAndStatus(10L, ApplicationStatus.SUBMITTED))
                .thenReturn(Collections.emptyList());

        Model model = new ConcurrentModel();
        String view = controller.index(null, model);

        assertEquals("admin/allocations/index", view);
        assertEquals(10L, model.getAttribute("selectedPeriodId"));
        assertEquals(period1, model.getAttribute("selectedPeriod"));
        assertEquals("allocations", model.getAttribute("activeMenu"));
    }

    @Test
    @DisplayName("preview() gọi allocationService.previewAndStore và chuyển hướng đến trang runDetail")
    void preview_success() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(adminUser);

        AllocationRun run = new AllocationRun();
        run.setId(88L);
        when(allocationService.previewAndStore(eq(10L), eq(1L))).thenReturn(run);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.preview(10L, authentication, redirectAttributes);

        assertEquals("redirect:/admin/allocations/runs/88", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
        verify(allocationService).previewAndStore(10L, 1L);
    }

    @Test
    @DisplayName("runDetail() nạp thông tin run và bảng xếp hạng items")
    void runDetail_success() {
        AllocationRun run = new AllocationRun();
        run.setId(88L);
        run.setPeriod(period1);
        run.setDryRun(true);
        run.setStatus(AllocationRunStatus.COMPLETED);

        AllocationItem item1 = new AllocationItem();
        item1.setRankNo(1);
        item1.setScore(1000);
        item1.setResult(AllocationResult.ASSIGNED);

        AllocationItem item2 = new AllocationItem();
        item2.setRankNo(2);
        item2.setScore(0);
        item2.setResult(AllocationResult.WAITLISTED);

        when(allocationService.getRun(88L)).thenReturn(run);
        when(allocationService.getRunItems(88L)).thenReturn(List.of(item1, item2));

        Model model = new ConcurrentModel();
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.runDetail(88L, model, redirectAttributes);

        assertEquals("admin/allocations/run_detail", view);
        assertEquals(run, model.getAttribute("run"));
        assertEquals(period1, model.getAttribute("period"));
        assertEquals(2, model.getAttribute("totalCount"));
        assertEquals(1L, model.getAttribute("assignedCount"));
        assertEquals(1L, model.getAttribute("waitlistCount"));
        assertEquals(0L, model.getAttribute("skippedCount"));
        assertNotNull(model.getAttribute("items"));
    }

    @Test
    @DisplayName("discardRun() gọi allocationService.discardRun và redirect về allocations trang chủ của period")
    void discardRun_success() {
        AllocationRun run = new AllocationRun();
        run.setId(88L);
        run.setPeriod(period1);

        when(allocationService.getRun(88L)).thenReturn(run);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.discardRun(88L, redirectAttributes);

        assertEquals("redirect:/admin/allocations?periodId=10", view);
        verify(allocationService).discardRun(88L);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    @DisplayName("commit() gọi allocationService.commit và redirect về kết quả lượt chạy")
    void commit_success() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(adminUser);

        AllocationRun run = new AllocationRun();
        run.setId(99L);
        when(allocationService.commit(eq(10L), eq(1L))).thenReturn(run);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.commit(10L, authentication, redirectAttributes);

        assertEquals("redirect:/admin/allocations/runs/99", view);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
        verify(allocationService).commit(10L, 1L);
    }

    @Test
    @DisplayName("commit() bắt BusinessException và lưu flash errorMessage")
    void commit_businessException() {
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUser()).thenReturn(adminUser);
        when(allocationService.commit(eq(10L), eq(1L))).thenThrow(new com.ktx.common.exception.BusinessException("Lỗi chốt phân bổ"));

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.commit(10L, authentication, redirectAttributes);

        assertEquals("redirect:/admin/allocations?periodId=10", view);
        assertEquals("Lỗi chốt phân bổ", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    @DisplayName("cancelDraft() gọi contractService.cancelDraft và redirect về runDetail nếu có runId")
    void cancelDraft_success() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.cancelDraft(50L, 99L, redirectAttributes);

        assertEquals("redirect:/admin/allocations/runs/99", view);
        verify(contractService).cancelDraft(50L);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    @DisplayName("cancelDraft() chuyển về /admin/allocations nếu không có runId")
    void cancelDraft_withoutRunId() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.cancelDraft(50L, null, redirectAttributes);

        assertEquals("redirect:/admin/allocations", view);
        verify(contractService).cancelDraft(50L);
        assertTrue(redirectAttributes.getFlashAttributes().containsKey("successMessage"));
    }

    @Test
    @DisplayName("cancelDraft() bắt BusinessException và lưu flash errorMessage")
    void cancelDraft_businessException() {
        doThrow(new com.ktx.common.exception.BusinessException("Không thể hủy HĐ")).when(contractService).cancelDraft(50L);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();
        String view = controller.cancelDraft(50L, 99L, redirectAttributes);

        assertEquals("redirect:/admin/allocations/runs/99", view);
        assertEquals("Không thể hủy HĐ", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }
}
