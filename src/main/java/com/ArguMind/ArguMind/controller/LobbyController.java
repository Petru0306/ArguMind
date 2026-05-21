package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.LobbyCreateRequestDto;
import com.ArguMind.ArguMind.dto.LobbyJoinRequestDto;
import com.ArguMind.ArguMind.dto.MatchResponseDto;
import com.ArguMind.ArguMind.model.GameMode;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.UserRepository;
import com.ArguMind.ArguMind.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lobbies")
@RequiredArgsConstructor
public class LobbyController {

    private final MatchService matchService;
    private final UserRepository userRepository;

    @PostMapping("/create")
    public ResponseEntity<MatchResponseDto> createLobby(@RequestBody LobbyCreateRequestDto request) {
        User user = currentUser();
        GameMode mode = request.getGameMode() != null ? request.getGameMode() : GameMode.STANDARD;
        String topic = request.getTopic() != null ? request.getTopic() : "Dezbatere ArguMind";
        return ResponseEntity.ok(matchService.createLobby(user.getId(), topic, mode));
    }

    @PostMapping("/join")
    public ResponseEntity<MatchResponseDto> joinLobby(@RequestBody LobbyJoinRequestDto request) {
        User user = currentUser();
        return ResponseEntity.ok(matchService.joinLobbyByCode(user.getId(), request.getCode()));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new org.springframework.security.authentication.AuthenticationCredentialsNotFoundException(
                    "Not authenticated");
        }
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + auth.getName()));
    }
}
