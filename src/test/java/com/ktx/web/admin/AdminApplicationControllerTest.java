package com.ktx.web.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import com.ktx.common.exception.BusinessException;
import com.ktx.domain.RegistrationPeriod;
import com.ktx.domain.RoomApplication;
import com.ktx.service.RegistrationPeriodService;
import com.ktx.service.RoomApplicationService;

@ExtendWith(MockitoExtension.class)
class AdminApplicationControllerTest {

    private AdminApplicationController controller;

    @Mock
    private RoomApplicationService roomApplicationService;

    @Mock
    private RegistrationPeriodService periodService;

    @BeforeEach
    void setUp() {
        controller = new AdminApplicationController(roomApplicationService, periodService);
    }

    @Test
    @DisplayName("list() mặc định chọn đợt đầu tiên nếu không truyền periodId")
    void list_DefaultFirstPeriod() {
        RegistrationPeriod p1 = new RegistrationPeriod();
        p1.setId(10L);
        RegistrationPeriod p2 = new RegistrationPeriod();
        p2.setId(20L);

        when(periodService.listAll()).thenReturn(List.of(p1, p2));
        when(roomApplicationService.listAllByPeriod(10L)).thenReturn(Collections.emptyList());

        Model model = new ConcurrentModel();
        String viewName = controller.list(null, model);

        assertEquals("admin/applications/list", viewName);
        assertEquals(10L, model.getAttribute("selectedPeriodId"));
        assertEquals(2, ((List<?>) model.getAttribute("periods")).size());
        assertEquals("applications", model.getAttribute("activeMenu"));
    }

    @Test
    @DisplayName("list() lọc theo periodId cụ thể được chỉ định")
    void list_WithExplicitPeriodId() {
        RegistrationPeriod p1 = new RegistrationPeriod();
        p1.setId(10L);

        RoomApplication app = new RoomApplication();
        app.setId(100L);

        when(periodService.listAll()).thenReturn(List.of(p1));
        when(roomApplicationService.listAllByPeriod(10L)).thenReturn(List.of(app));

        Model model = new ConcurrentModel();
        String viewName = controller.list(10L, model);

        assertEquals("admin/applications/list", viewName);
        assertEquals(10L, model.getAttribute("selectedPeriodId"));
        assertEquals(1, ((List<?>) model.getAttribute("applications")).size());
    }

    @Test
    @DisplayName("reject() thành công thì thêm thông báo success và redirect")
    void reject_Success() {
        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.reject(1L, 10L, redirectAttributes);

        verify(roomApplicationService).rejectApplication(1L);
        assertEquals("redirect:/admin/applications?periodId=10", view);
        assertEquals("Đã từ chối đơn đăng ký thành công", redirectAttributes.getFlashAttributes().get("successMessage"));
    }

    @Test
    @DisplayName("reject() gặp BusinessException thì thêm thông báo lỗi errorMessage và redirect")
    void reject_BusinessException() {
        doThrow(new BusinessException("Chỉ có thể từ chối đơn ở đợt mở hoặc đóng"))
                .when(roomApplicationService).rejectApplication(1L);

        RedirectAttributes redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.reject(1L, 10L, redirectAttributes);

        assertEquals("redirect:/admin/applications?periodId=10", view);
        assertEquals("Chỉ có thể từ chối đơn ở đợt mở hoặc đóng", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }
}
