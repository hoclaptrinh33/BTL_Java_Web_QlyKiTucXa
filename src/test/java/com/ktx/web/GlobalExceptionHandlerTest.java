package com.ktx.web;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleLockTimeout_withReferer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "/admin/allocations");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = handler.handleLockTimeout(request, redirectAttributes);

        assertEquals("redirect:/admin/allocations", view);
        assertEquals("Hệ thống đang phân bổ, thử lại", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }

    @Test
    void handleOptimisticLock_withoutReferer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = handler.handleOptimisticLock(request, redirectAttributes);

        assertEquals("redirect:/", view);
        assertEquals("Dữ liệu đã thay đổi, tải lại", redirectAttributes.getFlashAttributes().get("errorMessage"));
    }
}
