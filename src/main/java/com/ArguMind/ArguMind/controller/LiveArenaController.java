package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.GameEventDto;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.UserRepository;
import com.ArguMind.ArguMind.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class LiveArenaController {

    private final MatchService matchService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/match/{matchId}/action")
    public void handlePlayerAction(@DestinationVariable Long matchId,
                                   GameEventDto event,
                                   Principal principal) {
        if (principal == null) {
            throw new RuntimeException("Authentication required for arena actions");
        }

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        matchService.verifyParticipant(matchId, user.getId());

        event.setSenderUsername(principal.getName());

        if (event.getPayload() instanceof String text) {
            String cleanText = Jsoup.clean(text, Safelist.none()).trim();
            if (cleanText.isEmpty()) {
                return;
            }
            event.setPayload(cleanText);

            if (event.getType() == GameEventDto.EventType.SUBMIT) {
                matchService.processArgumentFromWebSocket(matchId, principal.getName(), cleanText);
            } else if (event.getType() == GameEventDto.EventType.TYPING) {
                messagingTemplate.convertAndSend("/topic/match/" + matchId, event);
            }
        }
    }
}
