package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.AiChatRequestDto;
import com.ArguMind.ArguMind.dto.AiChatResponseDto;
import com.ArguMind.ArguMind.service.AiDebateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-debate")
@RequiredArgsConstructor
public class AiDebateController {

    private final AiDebateService aiDebateService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponseDto> chat(@RequestBody AiChatRequestDto request) {
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(new AiChatResponseDto("Mesajul nu poate fi gol."));
        }
        String topic = request.getTopic() != null ? request.getTopic() : "Dezbatere generală";
        String reply = aiDebateService.chat(topic, request.getMessage().trim(), request.getHistory(),
                request.getPersonality());
        return ResponseEntity.ok(new AiChatResponseDto(reply));
    }
}
