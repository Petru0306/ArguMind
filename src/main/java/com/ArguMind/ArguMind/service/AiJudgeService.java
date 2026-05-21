package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.*;
import com.ArguMind.ArguMind.model.Argument;
import com.ArguMind.ArguMind.model.LogicalFallacy;
import com.ArguMind.ArguMind.model.Match;
import com.ArguMind.ArguMind.model.User;
import com.ArguMind.ArguMind.repository.ArgumentRepository;
import com.ArguMind.ArguMind.repository.LogicalFallacyRepository;
import com.ArguMind.ArguMind.repository.MatchRepository;
import com.ArguMind.ArguMind.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiJudgeService {

    private final MatchRepository matchRepository;
    private final ArgumentRepository argumentRepository;
    private final LogicalFallacyRepository logicalFallacyRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final EloCalculator eloCalculator;

    @Transactional
    public void evaluateMatch(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!"PROCESSING_AI".equals(match.getStatus())) {
            throw new RuntimeException("Match is not in PROCESSING_AI state");
        }

        List<Argument> arguments = argumentRepository.findByMatchIdOrderByRoundNumberAsc(matchId);
        
        // Construim payload-ul pentru AI
        String debateContext = arguments.stream()
                .map(arg -> String.format("Round %d - %s: %s", 
                        arg.getRoundNumber(), 
                        arg.getUser().getId().equals(match.getProUser().getId()) ? "PRO" : "CONTRA",
                        arg.getTextContent()))
                .collect(Collectors.joining("\n"));

        String systemPrompt = "You are an international academic debate judge. Evaluate the following debate on topic: " + match.getTopic() + "\n" +
                "Instructions:\n" +
                "1. Score each player (PRO and CONTRA) from 1-10 on logic, clarity, evidence, and rhetoric.\n" +
                "2. Detect logical fallacies (Ad Hominem, Strawman, Slippery Slope, etc.). For each, provide the player, round, fallacy name, offending text, and a brief explanation.\n" +
                "3. Determine the winner ('PRO' or 'CONTRA').\n" +
                "4. Output ONLY a valid JSON object matching this structure: \n" +
                "{ \"proScores\": { \"logic\": 0, \"clarity\": 0, \"evidence\": 0, \"rhetoric\": 0, \"total\": 0 }, " +
                "\"contraScores\": { \"logic\": 0, \"clarity\": 0, \"evidence\": 0, \"rhetoric\": 0, \"total\": 0 }, " +
                "\"fallacies\": [ { \"player\": \"PRO/CONTRA\", \"round\": 1, \"fallacyName\": \"\", \"offendingText\": \"\", \"explanation\": \"\" } ], " +
                "\"winner\": \"PRO/CONTRA\" }";

        try {
            // AICI s-ar face apelul real către API-ul LLM.
            // Pentru MVP / Testare, simulăm un răspuns JSON.
            String mockedAiResponse = simulateAiResponse(match);
            EvaluationResultDto result = objectMapper.readValue(mockedAiResponse, EvaluationResultDto.class);

            // Salvare erori logice
            for (FallacyDto fallacyDto : result.getFallacies()) {
                LogicalFallacy fallacy = LogicalFallacy.builder()
                        .match(match)
                        .player(fallacyDto.getPlayer())
                        .roundNumber(fallacyDto.getRound())
                        .fallacyName(fallacyDto.getFallacyName())
                        .offendingText(fallacyDto.getOffendingText())
                        .explanation(fallacyDto.getExplanation())
                        .build();
                logicalFallacyRepository.save(fallacy);
            }

            // Actualizare câștigător și status meci
            User winner = "PRO".equals(result.getWinner()) ? match.getProUser() : match.getContraUser();
            User loser = "PRO".equals(result.getWinner()) ? match.getContraUser() : match.getProUser();

            match.setWinner(winner);
            match.setStatus("FINISHED");
            matchRepository.save(match);

            // Calcul ELO Dinamic (Algoritm specific cerut în barem)
            EloResultDto eloResult = eloCalculator.calculateElo(
                match.getProUser().getEloRating(),
                match.getContraUser().getEloRating(),
                result.getWinner()
            );

            match.getProUser().setEloRating(eloResult.getNewProRating());
            match.getContraUser().setEloRating(eloResult.getNewContraRating());
            
            userRepository.save(match.getProUser());
            userRepository.save(match.getContraUser());

            // Notificăm finalizarea meciului și rezultatul via WebSocket
            messagingTemplate.convertAndSend("/topic/match/" + matchId, 
                GameEventDto.builder()
                        .type(GameEventDto.EventType.FINISHED)
                        .senderUsername("SYSTEM")
                        .payload(result)
                        .build());

        } catch (Exception e) {
            throw new RuntimeException("Failed to evaluate match with AI", e);
        }
    }

    private String simulateAiResponse(Match match) {
        // Un răspuns mockuit realist
        return "{\n" +
                "  \"proScores\": { \"logic\": 8, \"clarity\": 7, \"evidence\": 9, \"rhetoric\": 8, \"total\": 32 },\n" +
                "  \"contraScores\": { \"logic\": 6, \"clarity\": 8, \"evidence\": 5, \"rhetoric\": 7, \"total\": 26 },\n" +
                "  \"fallacies\": [\n" +
                "    {\n" +
                "      \"player\": \"CONTRA\",\n" +
                "      \"round\": 1,\n" +
                "      \"fallacyName\": \"Ad Hominem\",\n" +
                "      \"offendingText\": \"You are too young to understand this\",\n" +
                "      \"explanation\": \"Attacked the opponent's age instead of the argument.\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"winner\": \"PRO\"\n" +
                "}";
    }
}
