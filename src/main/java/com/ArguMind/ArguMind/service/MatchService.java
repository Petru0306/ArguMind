package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.*;
import com.ArguMind.ArguMind.model.Argument;
import com.ArguMind.ArguMind.model.GameMode;
import com.ArguMind.ArguMind.model.Match;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.ArgumentRepository;
import com.ArguMind.ArguMind.repository.MatchRepository;
import com.ArguMind.ArguMind.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ArgumentRepository argumentRepository;
    private final AiJudgeService aiJudgeService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final java.util.concurrent.locks.ReentrantLock matchmakingLock = new java.util.concurrent.locks.ReentrantLock();

    @Transactional
    public MatchResponseDto joinMatchmaking(MatchmakingRequestDto request) {
        matchmakingLock.lock();
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 1. Verificăm dacă userul are deja un meci PENDING sau ACTIVE pentru reconectare
            Optional<Match> ongoingMatch = matchRepository.findOngoingMatchForUser(user.getId());
            if (ongoingMatch.isPresent()) {
                return mapToResponseDto(ongoingMatch.get());
            }

            // 2. Caută orice meci PENDING global (nu ne mai pasă strict de temă la matchmaking general)
            // Ignorăm meciurile mai vechi de 1 oră (stale matches)
            return matchRepository.findFirstByStatusOrderByIdAsc("PENDING")
                    .map(match -> {
                        if (match.getProUser().getId().equals(user.getId())) {
                            return mapToResponseDto(match);
                        }
                        match.setContraUser(user);
                        match.setStatus("ACTIVE");
                        Match savedMatch = matchRepository.save(match);
                        
                        // Notificăm startul meciului via WebSocket DUPĂ COMMIT
                        MatchResponseDto response = mapToResponseDto(savedMatch);
                        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                            new org.springframework.transaction.support.TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    notifyMatchUpdate(savedMatch.getId(), GameEventDto.EventType.START, response);
                                }
                            }
                        );
                        
                        return response;
                    })
                    .orElseGet(() -> {
                        // 3. Dacă nu există, creează unul nou
                        Match newMatch = Match.builder()
                                .topic(request.getTopic() != null ? request.getTopic() : "Inteligența Artificială vs Umanitate")
                                .gameMode(request.getGameMode() != null ? request.getGameMode() : GameMode.STANDARD)
                                .proUser(user)
                                .status("PENDING")
                                .build();
                        return mapToResponseDto(matchRepository.save(newMatch));
                    });
        } finally {
            matchmakingLock.unlock();
        }
    }

    @Transactional
    public void submitArgument(Long matchId, ArgumentSubmitDto submitDto) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!"ACTIVE".equals(match.getStatus())) {
            throw new RuntimeException("Meciul nu este activ. Status curent: " + match.getStatus());
        }

        User user = userRepository.findById(submitDto.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Verifică dacă userul face parte din meci
        boolean isPro = match.getProUser().getId().equals(user.getId());
        boolean isContra = match.getContraUser() != null && match.getContraUser().getId().equals(user.getId());

        if (!isPro && !isContra) {
            throw new RuntimeException("User " + user.getUsername() + " is not part of this match");
        }

        // Determină rândul
        List<Argument> existingArguments = argumentRepository.findByMatchIdOrderByRoundNumberAsc(matchId);
        int argCount = existingArguments.size();

        // Runda 1: arg 0 (PRO), arg 1 (CONTRA)
        // Runda 2: arg 2 (PRO), arg 3 (CONTRA)
        boolean isProTurn = argCount % 2 == 0;

        if (isProTurn && !isPro) {
            throw new RuntimeException("It's PRO user's turn");
        }
        if (!isProTurn && !isContra) {
            throw new RuntimeException("It's CONTRA user's turn");
        }

        int roundNumber = (argCount / 2) + 1;

        Argument argument = Argument.builder()
                .match(match)
                .user(user)
                .textContent(submitDto.getTextContent())
                .roundNumber(roundNumber)
                .build();

        argumentRepository.save(argument);

        // Notificări asincrone după commit
        final MatchResponseDto response = mapToResponseDto(match);
        if (argCount + 1 >= 4) {
            match.setStatus("PROCESSING_AI");
            matchRepository.save(match);
            
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyMatchUpdate(matchId, GameEventDto.EventType.PROCESSING_AI, response);
                        eventPublisher.publishEvent(new MatchFinishedEvent(matchId));
                    }
                }
            );
        } else {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyMatchUpdate(matchId, GameEventDto.EventType.TURN_CHANGE, response);
                    }
                }
            );
        }
    }

    @Transactional
    public void processArgumentFromWebSocket(Long matchId, String username, String textContent) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        submitArgument(matchId, ArgumentSubmitDto.builder()
                .userId(user.getId())
                .textContent(textContent)
                .build());
    }

    private void notifyMatchUpdate(Long matchId, GameEventDto.EventType type, Object payload) {
        messagingTemplate.convertAndSend("/topic/match/" + matchId, 
                GameEventDto.builder()
                        .type(type)
                        .payload(payload)
                        .build());
    }

    @Transactional(readOnly = true)
    public MatchResponseDto getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        return mapToResponseDto(match);
    }

    private MatchResponseDto mapToResponseDto(Match match) {
        List<Argument> arguments = argumentRepository.findByMatchIdOrderByRoundNumberAsc(match.getId());
        int argCount = arguments.size();
        
        String currentTurn;
        int roundNumber;

        if ("PROCESSING_AI".equals(match.getStatus()) || "FINISHED".equals(match.getStatus())) {
            currentTurn = "NONE";
            roundNumber = 2; // Cap la 2 runde
        } else {
            currentTurn = (argCount % 2 == 0) ? "PRO" : "CONTRA";
            roundNumber = Math.min((argCount / 2) + 1, 2);
        }

        return MatchResponseDto.builder()
                .id(match.getId())
                .topic(match.getTopic())
                .status(match.getStatus())
                .gameMode(match.getGameMode())
                .initialTime(match.getGameMode().getTurnTimeSeconds())
                .proUserId(match.getProUser().getId())
                .contraUserId(match.getContraUser() != null ? match.getContraUser().getId() : null)
                .winnerId(match.getWinner() != null ? match.getWinner().getId() : null)
                .currentTurn(currentTurn)
                .roundNumber(roundNumber)
                .proLogicScore(match.getProLogicScore())
                .proClarityScore(match.getProClarityScore())
                .proRhetoricScore(match.getProRhetoricScore())
                .proEvidenceScore(match.getProEvidenceScore())
                .contraLogicScore(match.getContraLogicScore())
                .contraClarityScore(match.getContraClarityScore())
                .contraRhetoricScore(match.getContraRhetoricScore())
                .contraEvidenceScore(match.getContraEvidenceScore())
                .proEloChange(match.getProEloChange())
                .contraEloChange(match.getContraEloChange())
                .proFeedback(match.getProFeedback())
                .contraFeedback(match.getContraFeedback())
                .build();
    }
}
