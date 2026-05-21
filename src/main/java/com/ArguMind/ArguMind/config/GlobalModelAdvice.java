package com.ArguMind.ArguMind.config;

import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Arrays;
import java.util.stream.Collectors;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final UserRepository userRepository;

    @ModelAttribute("langSwitchRo")
    public String langSwitchRo(HttpServletRequest request) {
        return buildLangUrl(request, "ro");
    }

    @ModelAttribute("langSwitchEn")
    public String langSwitchEn(HttpServletRequest request) {
        return buildLangUrl(request, "en");
    }

    private static String buildLangUrl(HttpServletRequest request, String lang) {
        String path = request.getRequestURI();
        String query = request.getQueryString();
        if (query == null || query.isBlank()) {
            return path + "?lang=" + lang;
        }
        String cleaned = Arrays.stream(query.split("&"))
                .filter(p -> !p.isBlank() && !p.startsWith("lang="))
                .collect(Collectors.joining("&"));
        if (cleaned.isBlank()) {
            return path + "?lang=" + lang;
        }
        return path + "?" + cleaned + "&lang=" + lang;
    }

    @ModelAttribute("navUser")
    public User navUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName()).orElse(null);
    }
}
