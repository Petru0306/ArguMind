package com.ArguMind.ArguMind;

import com.ArguMind.ArguMind.dto.ArgumentSubmitDto;
import com.ArguMind.ArguMind.dto.MatchResponseDto;
import com.ArguMind.ArguMind.dto.MatchmakingRequestDto;
import com.ArguMind.ArguMind.dto.UserRegistrationDto;
import com.ArguMind.ArguMind.dto.UserRegistrationResponseDto;
import com.ArguMind.ArguMind.service.MatchService;
import com.ArguMind.ArguMind.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MatchServiceTests {

    @Autowired
    private MatchService matchService;

    @Autowired
    private UserService userService;

    @Test
    void testCompleteMatchFlow() throws InterruptedException {
        // 1. Înregistrare 2 useri
        UserRegistrationResponseDto user1 = userService.registerUser(new UserRegistrationDto("user_test_" + System.currentTimeMillis() + "_1", "pass1"));
        UserRegistrationResponseDto user2 = userService.registerUser(new UserRegistrationDto("user_test_" + System.currentTimeMillis() + "_2", "pass2"));

        String topic = "Is AI dangerous? " + System.currentTimeMillis();

        // 2. User1 dă join -> PENDING
        MatchResponseDto match1 = matchService.joinMatchmaking(MatchmakingRequestDto.builder()
                .userId(user1.getId())
                .topic(topic)
                .build());
        assertEquals("PENDING", match1.getStatus());
        assertEquals(user1.getId(), match1.getProUserId());
        assertNull(match1.getContraUserId());

        // 3. User2 dă join -> ACTIVE
        MatchResponseDto match2 = matchService.joinMatchmaking(MatchmakingRequestDto.builder()
                .userId(user2.getId())
                .topic(topic)
                .build());
        assertEquals("ACTIVE", match2.getStatus());
        assertEquals(user1.getId(), match2.getProUserId());
        assertEquals(user2.getId(), match2.getContraUserId());
        assertEquals(match1.getId(), match2.getId());

        Long matchId = match2.getId();

        // 4. Runde
        // Runda 1 - User1 (PRO)
        matchService.submitArgument(matchId, new ArgumentSubmitDto(user1.getId(), "First PRO argument"));
        
        // Runda 1 - User2 (CONTRA)
        matchService.submitArgument(matchId, new ArgumentSubmitDto(user2.getId(), "First CONTRA argument"));

        // Runda 2 - User1 (PRO)
        matchService.submitArgument(matchId, new ArgumentSubmitDto(user1.getId(), "Second PRO argument"));

        // Runda 2 - User2 (CONTRA) -> PROCESSING_AI -> FINISHED (via AI)
        matchService.submitArgument(matchId, new ArgumentSubmitDto(user2.getId(), "Second CONTRA argument"));

        // Așteptăm ca procesarea asincronă AI să se termine (max 10 secunde)
        int attempts = 0;
        MatchResponseDto finalMatch = matchService.getMatchById(matchId);
        while (!"FINISHED".equals(finalMatch.getStatus()) && attempts < 10) {
            Thread.sleep(1000);
            finalMatch = matchService.getMatchById(matchId);
            attempts++;
        }

        // 5. Verificare status final și efecte AI
        assertEquals("FINISHED", finalMatch.getStatus());
        assertNotNull(finalMatch.getWinnerId());
        System.out.println("Meciul s-a terminat. Câștigător ID: " + finalMatch.getWinnerId());

        // Verificăm ELO (Mock-ul nostru dă câștigător PRO/User1)
        UserRegistrationResponseDto updatedUser1 = userService.getUserById(user1.getId());
        UserRegistrationResponseDto updatedUser2 = userService.getUserById(user2.getId());

        assertEquals(1016, updatedUser1.getEloRating());
        assertEquals(984, updatedUser2.getEloRating());
        System.out.println("ELO actualizat dinamic: User1 (" + updatedUser1.getEloRating() + "), User2 (" + updatedUser2.getEloRating() + ")");
    }
}
