package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.*;
import com.ArguMind.ArguMind.model.*;
import com.ArguMind.ArguMind.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJudgeService {

    private final MatchRepository matchRepository;
    private final ArgumentRepository argumentRepository;
    private final LogicalFallacyRepository logicalFallacyRepository;
    private final UserRepository userRepository;
    private final DebateTopicRepository debateTopicRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final EloCalculator eloCalculator;

    @Value("${ai.api.key}")
    private String aiApiKey;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    private final RestClient restClient = RestClient.builder().build();

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMatchFinished(MatchFinishedEvent event) {
        evaluateMatch(event.getMatchId());
    }

    @Transactional
    public void evaluateMatch(Long matchId) {
        log.info("Starting AI evaluation for match: {}", matchId);
        
        if (aiApiKey == null || aiApiKey.isBlank() || "${AI_API_KEY:}".equals(aiApiKey)) {
            log.warn("AI API Key is missing or default! AI will fallback to DRAW. Please set AI_API_KEY environment variable.");
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!"PROCESSING_AI".equals(match.getStatus())) {
            log.warn("Match {} is not in PROCESSING_AI state. Current state: {}", matchId, match.getStatus());
            return;
        }

        try {
            List<Argument> arguments = argumentRepository.findByMatchIdOrderByRoundNumberAsc(matchId);
            DebateTopic topic = debateTopicRepository.findByTitle(match.getTopic())
                    .orElse(null);

            String systemPrompt = constructSystemPrompt();
            String userPrompt = constructUserPrompt(match, topic, arguments);

            EvaluationResultDto result = callGeminiApi(systemPrompt, userPrompt);
            processEvaluationResult(match, result);

        } catch (Exception e) {
            log.error("Failed to evaluate match: {}. Applying fallback DRAW.", matchId, e);
            applyFallback(match);
        }
    }

    private String constructSystemPrompt() {
        return "Ești un arbitru de debate de nivel academic, imparțial și riguros. " +
                "Sarcina ta este să analizezi un meci de debate PRO vs CONTRA și să oferi un verdict bazat pe: " +
                "Logică, Retorică, Claritate și Dovezi. " +
                "Trebuie să detectezi și erorile logice (Logical Fallacies) precum Ad Hominem, Strawman, Appeal to Emotion etc. " +
                "Pentru fiecare eroare logică detectată, trebuie să furnizezi CITATUL EXACT din textul jucătorului în câmpul 'offendingText'. " +
                "Răspunsul tău TREBUIE să fie un obiect JSON valid care să respecte strict următoarea structură: " +
                "{ \"proScores\": { \"logic\": int, \"clarity\": int, \"evidence\": int, \"rhetoric\": int, \"total\": int, \"feedback\": \"string\" }, " +
                "  \"contraScores\": { \"logic\": int, \"clarity\": int, \"evidence\": int, \"rhetoric\": int, \"total\": int, \"feedback\": \"string\" }, " +
                "  \"fallacies\": [ { \"player\": \"PRO\"|\"CONTRA\", \"round\": int, \"fallacyName\": \"string\", \"offendingText\": \"string\", \"explanation\": \"string\" } ], " +
                "  \"winner\": \"PRO\"|\"CONTRA\" }";
    }

    private String constructUserPrompt(Match match, DebateTopic topic, List<Argument> arguments) {
        StringBuilder sb = new StringBuilder();
        sb.append("Temă: ").append(match.getTopic()).append("\n");
        if (topic != null) {
            sb.append("Categorie: ").append(topic.getCategory()).append("\n");
        }
        sb.append("Argumente:\n");
        for (Argument arg : arguments) {
            String role = arg.getUser().getId().equals(match.getProUser().getId()) ? "PRO" : "CONTRA";
            sb.append(String.format("Runda %d - %s: %s\n", arg.getRoundNumber(), role, arg.getTextContent()));
        }
        return sb.toString();
    }

    private EvaluationResultDto callGeminiApi(String systemPrompt, String userPrompt) throws Exception {
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(Map.of("text", systemPrompt + "\n\nAnalizează următorul meci:\n" + userPrompt)))
                ),
                "generationConfig", Map.of(
                        "response_mime_type", "application/json"
                )
        );

        String response = restClient.post()
                .uri(aiApiUrl + "?key=" + aiApiKey)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        JsonNode root = objectMapper.readTree(response);
        String jsonText = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        
        return objectMapper.readValue(jsonText, EvaluationResultDto.class);
    }

    private void processEvaluationResult(Match match, EvaluationResultDto result) {
        // Salvare erori logice
        if (result.getFallacies() != null) {
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
        }

        // Actualizare câștigător și status meci
        User winner = "PRO".equalsIgnoreCase(result.getWinner()) ? match.getProUser() : match.getContraUser();
        match.setWinner(winner);
        match.setStatus("FINISHED");

        // Salvare scoruri
        updateMatchScores(match, result);

        // Calcul ELO
        EloResultDto eloResult = eloCalculator.calculateElo(
                match.getProUser().getEloRating(),
                match.getContraUser().getEloRating(),
                result.getWinner().toUpperCase()
        );

        updateUserElo(match, eloResult);
        matchRepository.save(match);

        // Notificăm finalizarea
        sendFinishEvent(match.getId(), result);
        log.info("Finished AI evaluation for match: {}", match.getId());
    }

    private void updateMatchScores(Match match, EvaluationResultDto result) {
        if (result.getProScores() != null) {
            match.setProLogicScore(result.getProScores().getLogic());
            match.setProClarityScore(result.getProScores().getClarity());
            match.setProRhetoricScore(result.getProScores().getRhetoric());
            match.setProEvidenceScore(result.getProScores().getEvidence());
            match.setProFeedback(result.getProScores().getFeedback());
        }
        if (result.getContraScores() != null) {
            match.setContraLogicScore(result.getContraScores().getLogic());
            match.setContraClarityScore(result.getContraScores().getClarity());
            match.setContraRhetoricScore(result.getContraScores().getRhetoric());
            match.setContraEvidenceScore(result.getContraScores().getEvidence());
            match.setContraFeedback(result.getContraScores().getFeedback());
        }
    }

    private void updateUserElo(Match match, EloResultDto eloResult) {
        match.setProEloChange(eloResult.getNewProRating() - match.getProUser().getEloRating());
        match.setContraEloChange(eloResult.getNewContraRating() - match.getContraUser().getEloRating());

        match.getProUser().setEloRating(eloResult.getNewProRating());
        match.getContraUser().setEloRating(eloResult.getNewContraRating());

        userRepository.save(match.getProUser());
        userRepository.save(match.getContraUser());
    }

    private void applyFallback(Match match) {
        EvaluationResultDto fallbackResult = EvaluationResultDto.builder()
                .winner("DRAW")
                .proScores(PlayerScoreDto.builder().logic(5).clarity(5).rhetoric(5).evidence(5).total(20).feedback("Eroare tehnică AI. Meciul a fost arbitrat ca egalitate.").build())
                .contraScores(PlayerScoreDto.builder().logic(5).clarity(5).rhetoric(5).evidence(5).total(20).feedback("Eroare tehnică AI. Meciul a fost arbitrat ca egalitate.").build())
                .fallacies(List.of())
                .build();

        match.setStatus("FINISHED");
        match.setWinner(null); // Draw
        updateMatchScores(match, fallbackResult);

        EloResultDto eloResult = eloCalculator.calculateElo(
                match.getProUser().getEloRating(),
                match.getContraUser().getEloRating(),
                "DRAW"
        );

        updateUserElo(match, eloResult);
        matchRepository.save(match);

        sendFinishEvent(match.getId(), fallbackResult);
    }

    private void sendFinishEvent(Long matchId, EvaluationResultDto result) {
        messagingTemplate.convertAndSend("/topic/match/" + matchId,
                GameEventDto.builder()
                        .type(GameEventDto.EventType.FINISHED)
                        .senderUsername("SYSTEM")
                        .payload(result)
                        .build());
    }
}
