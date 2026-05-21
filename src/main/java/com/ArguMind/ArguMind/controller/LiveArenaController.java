package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.ArgumentSubmitDto;
import com.ArguMind.ArguMind.dto.GameEventDto;
import com.ArguMind.ArguMind.service.MatchService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class LiveArenaController {

    private final MatchService matchService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    /**
     * Gestionează acțiunile jucătorilor în arenă (ex: typing, submission).
     * Am eliminat @SendTo pentru a preveni Race Condition între SUBMIT și TURN_CHANGE.
     */
    @MessageMapping("/match/{matchId}/action")
    public void handlePlayerAction(@DestinationVariable Long matchId, 
                                   GameEventDto event, 
                                   Principal principal) {
        
        event.setSenderUsername(principal.getName());

        if (event.getPayload() instanceof String text) {
            String cleanText = Jsoup.clean(text, Safelist.none());
            event.setPayload(cleanText);

            if (event.getType() == GameEventDto.EventType.SUBMIT) {
                // MatchService va face broadcast la TURN_CHANGE sau PROCESSING_AI
                matchService.processArgumentFromWebSocket(matchId, principal.getName(), cleanText);
            } else if (event.getType() == GameEventDto.EventType.TYPING) {
                // Pentru TYPING facem broadcast manual
                messagingTemplate.convertAndSend("/topic/match/" + matchId, event);
            }
        }
    }
}
