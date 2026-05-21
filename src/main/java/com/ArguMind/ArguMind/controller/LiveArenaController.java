package com.ArguMind.ArguMind.controller;

import com.ArguMind.ArguMind.dto.GameEventDto;
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

    /**
     * Gestionează acțiunile jucătorilor în arenă (ex: typing, submission).
     * Include logică de programare defensivă pentru prevenirea atacurilor XSS.
     */
    @MessageMapping("/match/{matchId}/action")
    @SendTo("/topic/match/{matchId}")
    public GameEventDto handlePlayerAction(@DestinationVariable Long matchId, 
                                           GameEventDto event, 
                                           Principal principal) {
        
        // Securitate: Atribuim automat sender-ul din Principal (userul logat)
        event.setSenderUsername(principal.getName());

        // Programare Defensivă: Curățăm payload-ul de eventuale scripturi malicioase (XSS)
        if (event.getPayload() instanceof String text) {
            String cleanText = Jsoup.clean(text, Safelist.none());
            event.setPayload(cleanText);
        }

        return event;
    }
}
