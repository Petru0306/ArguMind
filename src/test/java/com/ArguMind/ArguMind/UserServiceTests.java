package com.ArguMind.ArguMind;

import com.ArguMind.ArguMind.dto.UserRegistrationDto;
import com.ArguMind.ArguMind.dto.UserRegistrationResponseDto;
import com.ArguMind.ArguMind.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTests {

    @Autowired
    private UserService userService;

    @Test
    void testCreateUser() {
        String suffix = String.valueOf(System.currentTimeMillis());
        UserRegistrationDto dto = UserRegistrationDto.builder()
                .username("user_" + suffix)
                .email("user_" + suffix + "@test.local")
                .password("parola123")
                .confirmPassword("parola123")
                .build();

        UserRegistrationResponseDto user = userService.registerUser(dto);

        assertNotNull(user.getId());
        assertEquals(dto.getUsername(), user.getUsername());
        assertEquals(dto.getEmail(), user.getEmail());
    }
}
