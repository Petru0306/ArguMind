package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.ArgumentSubmitDto;
import com.ArguMind.ArguMind.dto.MatchResponseDto;
import com.ArguMind.ArguMind.dto.MatchmakingRequestDto;
import com.ArguMind.ArguMind.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/join")
    public ResponseEntity<MatchResponseDto> joinMatchmaking(@RequestBody MatchmakingRequestDto request) {
        return ResponseEntity.ok(matchService.joinMatchmaking(request));
    }

    @PostMapping("/{id}/arguments")
    public ResponseEntity<Void> submitArgument(@PathVariable Long id, @RequestBody ArgumentSubmitDto submitDto) {
        matchService.submitArgument(id, submitDto);
        return ResponseEntity.ok().build();
    }
}
