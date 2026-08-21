package com.ktx.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.ktx.security.LoginSuccessHandler;

@Controller
public class HomeController {

    private final LoginSuccessHandler loginSuccessHandler;

    public HomeController(LoginSuccessHandler loginSuccessHandler) {
        this.loginSuccessHandler = loginSuccessHandler;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        return "redirect:" + loginSuccessHandler.resolveTarget(authentication);
    }
}
