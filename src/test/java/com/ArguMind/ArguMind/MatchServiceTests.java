package com.ArguMind.ArguMind;

import com.ArguMind.ArguMind.dto.ArgumentSubmitDto;
import com.ArguMind.ArguMind.dto.MatchResponseDto;
import com.ArguMind.ArguMind.dto.MatchmakingRequestDto;
import com.ArguMind.ArguMind.dto.UserRegistrationDto;
import com.ArguMind.ArguMind.dto.UserRegistrationResponseDto;
import com.ArguMind.ArguMind.model.GameMode;
import com.ArguMind.ArguMind.service.MatchService;
import com.ArguMind.ArguMind.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class MatchServiceTests {

    @Autowired
    private MatchService matchService;

    @Autowired
    private UserService userService;

    private UserRegistrationResponseDto registerTestUser(String suffix) {
        return userService.registerUser(UserRegistrationDto.builder()
                .username("user_test_" + suffix)
                .email("user_test_" + suffix + "@test.local")
                .password("password12")
                .confirmPassword("password12")
                .build());
    }

    @Test
    @Transactional
    void testLobbyCreateAndJoinByCode() {
        String suffix = String.valueOf(System.currentTimeMillis());
        UserRegistrationResponseDto host = registerTestUser(suffix + "_host");
        UserRegistrationResponseDto guest = registerTestUser(suffix + "_guest");

        MatchResponseDto lobby = matchService.createLobby(host.getId(), "Lobby test", GameMode.RAPID);
        assertEquals("PENDING", lobby.getStatus());
        assertNotNull(lobby.getJoinCode());
        assertEquals(6, lobby.getJoinCode().length());

        MatchResponseDto joined = matchService.joinLobbyByCode(guest.getId(), lobby.getJoinCode());
        assertEquals("ACTIVE", joined.getStatus());
        assertEquals(guest.getId(), joined.getContraUserId());
        assertEquals(lobby.getId(), joined.getId());
    }

    @Test
    @Transactional
    void testMatchmakingQueueAndJoin() {
        String suffix = String.valueOf(System.currentTimeMillis());
        UserRegistrationResponseDto user1 = registerTestUser(suffix + "_1");
        UserRegistrationResponseDto user2 = registerTestUser(suffix + "_2");

        MatchResponseDto pending = matchService.joinMatchmaking(MatchmakingRequestDto.builder()
                .userId(user1.getId())
                .topic("Test topic " + suffix)
                .gameMode(GameMode.STANDARD)
                .build());

        assertEquals("PENDING", pending.getStatus());
        assertEquals(user1.getId(), pending.getProUserId());
        assertNull(pending.getContraUserId());

        MatchResponseDto active = matchService.joinMatchmaking(MatchmakingRequestDto.builder()
                .userId(user2.getId())
                .topic("Other topic")
                .gameMode(GameMode.STANDARD)
                .build());

        assertEquals("ACTIVE", active.getStatus());
        assertEquals(user2.getId(), active.getContraUserId());
        assertEquals(pending.getId(), active.getId());
    }

    @Test
    void testCompleteMatchFlow() throws InterruptedException {
        String suffix = String.valueOf(System.currentTimeMillis());
        UserRegistrationResponseDto user1 = registerTestUser(suffix + "_1");
        UserRegistrationResponseDto user2 = registerTestUser(suffix + "_2");

        String topic = "Is AI dangerous? " + suffix;

        matchService.joinMatchmaking(MatchmakingRequestDto.builder()
                .userId(user1.getId())
                .topic(topic)
                .gameMode(GameMode.STANDARD)
                .build());

        MatchResponseDto match2 = matchService.joinMatchmaking(MatchmakingRequestDto.builder()
                .userId(user2.getId())
                .topic(topic)
                .gameMode(GameMode.STANDARD)
                .build());

        Long matchId = match2.getId();

        matchService.submitArgument(matchId, new ArgumentSubmitDto(user1.getId(), "First PRO argument"));
        matchService.submitArgument(matchId, new ArgumentSubmitDto(user2.getId(), "First CONTRA argument"));
        matchService.submitArgument(matchId, new ArgumentSubmitDto(user1.getId(), "Second PRO argument"));
        matchService.submitArgument(matchId, new ArgumentSubmitDto(user2.getId(), "Second CONTRA argument"));

        int attempts = 0;
        MatchResponseDto finalMatch = matchService.getMatchById(matchId);
        while (!"FINISHED".equals(finalMatch.getStatus()) && attempts < 15) {
            Thread.sleep(500);
            finalMatch = matchService.getMatchById(matchId);
            attempts++;
        }

        assertEquals("FINISHED", finalMatch.getStatus());
    }
}
