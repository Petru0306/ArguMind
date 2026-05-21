package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.ArgumentSubmitDto;
import com.ArguMind.ArguMind.dto.MatchResponseDto;
import com.ArguMind.ArguMind.dto.MatchmakingRequestDto;
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
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;
    private final UserRepository userRepository;

    @PostMapping("/join")
    public ResponseEntity<MatchResponseDto> joinMatchmaking(@RequestBody MatchmakingRequestDto request) {
        User user = currentUser();
        request.setUserId(user.getId());
        if (request.getGameMode() == null) {
            request.setGameMode(GameMode.STANDARD);
        }
        return ResponseEntity.ok(matchService.joinMatchmaking(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatchResponseDto> getMatch(@PathVariable Long id) {
        User user = currentUser();
        MatchResponseDto match = matchService.getMatchById(id);
        matchService.verifyParticipant(id, user.getId());
        return ResponseEntity.ok(match);
    }

    @PostMapping("/{id}/arguments")
    public ResponseEntity<Void> submitArgument(@PathVariable Long id, @RequestBody ArgumentSubmitDto submitDto) {
        User user = currentUser();
        submitDto.setUserId(user.getId());
        matchService.submitArgument(id, submitDto);
        return ResponseEntity.ok().build();
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
