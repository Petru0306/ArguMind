package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.FriendRequestDto;
import com.ArguMind.ArguMind.repository.UserRepository;
import com.ArguMind.ArguMind.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendController {

    private final FriendService friendService;
    private final UserRepository userRepository;

    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestBody FriendRequestDto body) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Autentificare necesară."));
        }
        try {
            friendService.sendFriendRequest(userId, body.getUsername());
            return ResponseEntity.ok(Map.of("message", "Cerere de prietenie trimisă."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/accept/{friendshipId}")
    public ResponseEntity<?> accept(@PathVariable Long friendshipId) {
        Long userId = currentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Autentificare necesară."));
        }
        try {
            friendService.acceptFriendRequest(friendshipId, userId);
            return ResponseEntity.ok(Map.of("message", "Prieten adăugat."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName()).map(u -> u.getId()).orElse(null);
    }
}
