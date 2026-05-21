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
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private static final int STALE_PENDING_HOURS = 1;
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;

    private final SecureRandom secureRandom = new SecureRandom();
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ArgumentRepository argumentRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final ReentrantLock matchmakingLock = new ReentrantLock();

    @Transactional
    public MatchResponseDto createLobby(Long userId, String topic, GameMode gameMode) {
        matchmakingLock.lock();
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Optional<Match> ongoing = matchRepository.findOngoingMatchForUser(user.getId());
            if (ongoing.isPresent() && ongoing.get().getJoinCode() != null) {
                return mapToResponseDto(ongoing.get());
            }

            Match lobby = Match.builder()
                    .topic(topic)
                    .gameMode(gameMode != null ? gameMode : GameMode.STANDARD)
                    .proUser(user)
                    .status("PENDING")
                    .joinCode(generateUniqueJoinCode())
                    .build();
            return mapToResponseDto(matchRepository.save(lobby));
        } finally {
            matchmakingLock.unlock();
        }
    }

    @Transactional
    public MatchResponseDto joinLobbyByCode(Long userId, String code) {
        matchmakingLock.lock();
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String normalized = normalizeLobbyCode(code);
            if (normalized == null) {
                throw new IllegalArgumentException("Introdu un cod de lobby valid (6 caractere).");
            }

            Match match = matchRepository.findByJoinCodeIgnoreCaseAndStatus(normalized, "PENDING")
                    .orElseThrow(() -> new IllegalArgumentException("Lobby inexistent sau deja pornit. Verifică codul."));

            if (isParticipant(match, user.getId())) {
                return mapToResponseDto(match);
            }
            if (countPlayers(match) >= match.getGameMode().getRequiredPlayers()) {
                throw new IllegalArgumentException("Lobby-ul este deja plin.");
            }

            return joinExistingPendingMatch(match, user);
        } finally {
            matchmakingLock.unlock();
        }
    }

    @Transactional
    public MatchResponseDto joinMatchmaking(MatchmakingRequestDto request) {
        matchmakingLock.lock();
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            cleanupStalePendingMatches();

            Optional<Match> ongoingMatch = matchRepository.findOngoingMatchForUser(user.getId());
            if (ongoingMatch.isPresent()) {
                return mapToResponseDto(ongoingMatch.get());
            }

            GameMode gameMode = request.getGameMode() != null ? request.getGameMode() : GameMode.STANDARD;
            String topic = request.getTopic() != null && !request.getTopic().isBlank()
                    ? request.getTopic()
                    : "Inteligența Artificială vs Umanitate";

            return matchRepository.findJoinablePendingMatch(user.getId(), gameMode)
                    .map(match -> joinExistingPendingMatch(match, user))
                    .orElseGet(() -> createPendingMatch(user, topic, gameMode));
        } finally {
            matchmakingLock.unlock();
        }
    }

    private MatchResponseDto joinExistingPendingMatch(Match match, User user) {
        if (isParticipant(match, user.getId())) {
            return mapToResponseDto(match);
        }

        assignNextLobbySlot(match, user);
        int required = match.getGameMode().getRequiredPlayers();
        if (countPlayers(match) >= required) {
            match.setStatus("ACTIVE");
        }
        Match savedMatch = matchRepository.save(match);

        MatchResponseDto response = mapToResponseDto(savedMatch);
        if ("ACTIVE".equals(savedMatch.getStatus())) {
            registerAfterCommitNotification(savedMatch.getId(), GameEventDto.EventType.START, response);
        }
        return response;
    }

    private void assignNextLobbySlot(Match match, User user) {
        if (match.getGameMode() == GameMode.TEAMS_2V2) {
            if (match.getProUser2() == null) {
                match.setProUser2(user);
            } else if (match.getContraUser() == null) {
                match.setContraUser(user);
            } else if (match.getContraUser2() == null) {
                match.setContraUser2(user);
            } else {
                throw new IllegalArgumentException("Lobby-ul este deja plin.");
            }
        } else if (match.getContraUser() == null) {
            match.setContraUser(user);
        } else {
            throw new IllegalArgumentException("Lobby-ul este deja plin.");
        }
    }

    private MatchResponseDto createPendingMatch(User user, String topic, GameMode gameMode) {
        Match newMatch = Match.builder()
                .topic(topic)
                .gameMode(gameMode)
                .proUser(user)
                .status("PENDING")
                .build();
        return mapToResponseDto(matchRepository.save(newMatch));
    }

    private void cleanupStalePendingMatches() {
        try {
            Instant cutoff = Instant.now().minus(STALE_PENDING_HOURS, ChronoUnit.HOURS);
            matchRepository.deleteStalePendingMatches(cutoff);
        } catch (Exception e) {
            log.warn("Stale match cleanup skipped: {}", e.getMessage());
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

        if (!isParticipant(match, user.getId())) {
            throw new RuntimeException("User " + user.getUsername() + " is not part of this match");
        }

        List<Argument> existingArguments = argumentRepository.findByMatchIdOrderByRoundNumberAsc(matchId);
        int argCount = existingArguments.size();
        Long expectedUserId = resolveCurrentTurnUserId(match, argCount);

        if (!user.getId().equals(expectedUserId)) {
            throw new RuntimeException("Nu este rândul tău.");
        }

        int roundNumber = match.getGameMode() == GameMode.TEAMS_2V2
                ? argCount + 1
                : (argCount / 2) + 1;

        Argument argument = Argument.builder()
                .match(match)
                .user(user)
                .textContent(submitDto.getTextContent())
                .roundNumber(roundNumber)
                .build();

        argumentRepository.save(argument);

        final MatchResponseDto response = mapToResponseDto(match);
        int totalArgs = match.getGameMode().getTotalArguments();
        if (argCount + 1 >= totalArgs) {
            match.setStatus("PROCESSING_AI");
            matchRepository.save(match);

            registerAfterCommitNotification(matchId, GameEventDto.EventType.PROCESSING_AI, response);
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publishEvent(new MatchFinishedEvent(matchId));
                        }
                    }
            );
        } else {
            registerAfterCommitNotification(matchId, GameEventDto.EventType.TURN_CHANGE, response);
        }
    }

    @Transactional
    public void processArgumentFromWebSocket(Long matchId, String username, String textContent) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        verifyParticipant(matchId, user.getId());

        submitArgument(matchId, ArgumentSubmitDto.builder()
                .userId(user.getId())
                .textContent(textContent)
                .build());
    }

    public void verifyParticipant(Long matchId, Long userId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!isParticipant(match, userId)) {
            throw new RuntimeException("User is not part of this match");
        }
    }

    private boolean isParticipant(Match match, Long userId) {
        if (match.getProUser() != null && match.getProUser().getId().equals(userId)) {
            return true;
        }
        if (match.getProUser2() != null && match.getProUser2().getId().equals(userId)) {
            return true;
        }
        if (match.getContraUser() != null && match.getContraUser().getId().equals(userId)) {
            return true;
        }
        return match.getContraUser2() != null && match.getContraUser2().getId().equals(userId);
    }

    private int countPlayers(Match match) {
        int n = match.getProUser() != null ? 1 : 0;
        if (match.getProUser2() != null) n++;
        if (match.getContraUser() != null) n++;
        if (match.getContraUser2() != null) n++;
        return n;
    }

    private Long resolveCurrentTurnUserId(Match match, int argCount) {
        if (match.getGameMode() == GameMode.TEAMS_2V2) {
            return switch (argCount % 4) {
                case 0 -> match.getProUser().getId();
                case 1 -> match.getProUser2().getId();
                case 2 -> match.getContraUser().getId();
                case 3 -> match.getContraUser2().getId();
                default -> match.getProUser().getId();
            };
        }
        boolean proTurn = argCount % 2 == 0;
        return proTurn ? match.getProUser().getId() : match.getContraUser().getId();
    }

    private String resolveCurrentTurnLabel(Match match, int argCount) {
        if (match.getGameMode() == GameMode.TEAMS_2V2) {
            return switch (argCount % 4) {
                case 0 -> "PRO";
                case 1 -> "PRO2";
                case 2 -> "CONTRA";
                case 3 -> "CONTRA2";
                default -> "PRO";
            };
        }
        return (argCount % 2 == 0) ? "PRO" : "CONTRA";
    }

    private boolean isProSide(Match match, Long userId) {
        return (match.getProUser() != null && match.getProUser().getId().equals(userId))
                || (match.getProUser2() != null && match.getProUser2().getId().equals(userId));
    }

    private void registerAfterCommitNotification(Long matchId, GameEventDto.EventType type, Object payload) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        notifyMatchUpdate(matchId, type, payload);
                    }
                }
        );
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

        if ("PENDING".equals(match.getStatus())) {
            currentTurn = "NONE";
            roundNumber = 1;
        } else if ("PROCESSING_AI".equals(match.getStatus()) || "FINISHED".equals(match.getStatus())) {
            currentTurn = "NONE";
            roundNumber = 2;
        } else {
            currentTurn = resolveCurrentTurnLabel(match, argCount);
            roundNumber = match.getGameMode() == GameMode.TEAMS_2V2
                    ? Math.min(argCount + 1, 4)
                    : Math.min((argCount / 2) + 1, 2);
        }

        Long turnUserId = "NONE".equals(currentTurn) ? null : resolveCurrentTurnUserId(match, argCount);

        return MatchResponseDto.builder()
                .id(match.getId())
                .joinCode(match.getJoinCode())
                .topic(match.getTopic())
                .status(match.getStatus())
                .gameMode(match.getGameMode())
                .initialTime(match.getGameMode().getTurnTimeSeconds())
                .proUserId(match.getProUser().getId())
                .proUser2Id(match.getProUser2() != null ? match.getProUser2().getId() : null)
                .contraUserId(match.getContraUser() != null ? match.getContraUser().getId() : null)
                .contraUser2Id(match.getContraUser2() != null ? match.getContraUser2().getId() : null)
                .currentTurnUserId(turnUserId)
                .requiredPlayers(match.getGameMode().getRequiredPlayers())
                .playersJoined(countPlayers(match))
                .winnerId(match.getWinner() != null ? match.getWinner().getId() : null)
                .currentTurn(currentTurn)
                .roundNumber(roundNumber)
                .arguments(mapArgumentsToDto(match, arguments))
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

    private List<ArgumentViewDto> mapArgumentsToDto(Match match, List<Argument> arguments) {
        return arguments.stream()
                .map(arg -> ArgumentViewDto.builder()
                        .side(isProSide(match, arg.getUser().getId()) ? "PRO" : "CONTRA")
                        .author(arg.getUser().getUsername())
                        .roundNumber(arg.getRoundNumber())
                        .textContent(arg.getTextContent())
                        .build())
                .toList();
    }

    private String generateUniqueJoinCode() {
        for (int attempt = 0; attempt < 30; attempt++) {
            StringBuilder sb = new StringBuilder(CODE_LENGTH);
            for (int i = 0; i < CODE_LENGTH; i++) {
                sb.append(CODE_CHARS.charAt(secureRandom.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            if (!matchRepository.existsByJoinCode(code)) {
                return code;
            }
        }
        throw new RuntimeException("Nu s-a putut genera codul lobby-ului. Încearcă din nou.");
    }

    private String normalizeLobbyCode(String code) {
        if (code == null) {
            return null;
        }
        String normalized = code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return normalized.length() == CODE_LENGTH ? normalized : null;
    }
}
