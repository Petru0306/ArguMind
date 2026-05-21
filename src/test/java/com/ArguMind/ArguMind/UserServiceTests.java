package com.ArguMind.ArguMind;

import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTests {

    @Autowired
    private UserService userService;

    @Test
    void testCreateUser() {
        String username = "mihai2718";
        User user = userService.createUser(username, "parola123");
        
        assertNotNull(user.getId());
        assertEquals(username, user.getUsername());
        System.out.println("Utilizator creat cu succes: " + user.getUsername() + " cu ID: " + user.getId());
    }
}
