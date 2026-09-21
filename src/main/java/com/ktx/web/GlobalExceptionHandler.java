package com.ktx.web;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class})
    public String handleLockTimeout(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Hệ thống đang phân bổ, thử lại");
        String referer = request.getHeader("Referer");
        return (referer != null && !referer.isBlank()) ? "redirect:" + referer : "redirect:/";
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String handleOptimisticLock(HttpServletRequest request, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("errorMessage", "Dữ liệu đã thay đổi, tải lại");
        String referer = request.getHeader("Referer");
        return (referer != null && !referer.isBlank()) ? "redirect:" + referer : "redirect:/";
    }
}
