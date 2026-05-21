package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.UserRegistrationDto;
import com.ArguMind.ArguMind.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class ViewController {

    private final UserService userService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /**
     * Mapare temporară pentru POST /login pentru a evita eroarea 405 
     * în timpul previzualizării cu securitatea dezactivată.
     */
    @PostMapping("/login")
    public String loginPost() {
        return "redirect:/dashboard";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute UserRegistrationDto registrationDto) {
        userService.registerUser(registrationDto);
        return "redirect:/login?success";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
