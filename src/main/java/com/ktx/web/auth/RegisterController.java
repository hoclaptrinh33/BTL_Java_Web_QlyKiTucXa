package com.ktx.web.auth;

import jakarta.validation.Valid;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.ktx.common.exception.DuplicateFieldException;
import com.ktx.dto.RegisterForm;
import com.ktx.service.AuthService;

@Controller
public class RegisterController {

    private final AuthService authService;

    public RegisterController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/register")
    public String form(@ModelAttribute("form") RegisterForm form, Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String submit(@Valid @ModelAttribute("form") RegisterForm form, BindingResult binding,
            Authentication authentication) {
        if (isAuthenticated(authentication)) {
            return "redirect:/";
        }
        if (form.getPassword() != null && !form.getPassword().equals(form.getConfirmPassword())) {
            binding.rejectValue("confirmPassword", "mismatch", "Xác nhận mật khẩu không khớp");
        }
        if (binding.hasErrors()) {
            return "auth/register";
        }
        try {
            authService.register(form);
        } catch (DuplicateFieldException ex) {
            binding.rejectValue(ex.getField(), "duplicate", ex.getMessage());
            return "auth/register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/register/google")
    public String google() {
        return "redirect:/login?google";
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
