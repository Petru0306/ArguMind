package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.*;
import com.ArguMind.ArguMind.model.*;
import com.ArguMind.ArguMind.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiJudgeService {

    private static final List<String> GEMINI_MODELS = List.of(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash-lite"
    );
    private static final Pattern JSON_BLOCK = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)```", Pattern.CASE_INSENSITIVE);

    private final MatchRepository matchRepository;
    private final ArgumentRepository argumentRepository;
    private final LogicalFallacyRepository logicalFallacyRepository;
    private final UserRepository userRepository;
    private final DebateTopicRepository debateTopicRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final EloCalculator eloCalculator;
    private final RankService rankService;

    @Value("${ai.api.key:}")
    private String aiApiKey;

    private final RestClient restClient = RestClient.builder().build();

    @PostConstruct
    void logAiConfig() {
        String key = effectiveApiKey();
        if (key.isEmpty()) {
            log.warn("Gemini API key lipsește — verifică application-secrets.properties (ai.api.key)");
        } else {
            log.info("Gemini API key încărcată ({} caractere)", key.length());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMatchFinished(MatchFinishedEvent event) {
        evaluateMatch(event.getMatchId());
    }

    @Transactional
    public void evaluateMatch(Long matchId) {
        log.info("Starting AI evaluation for match: {}", matchId);

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (!"PROCESSING_AI".equals(match.getStatus())) {
            log.warn("Match {} is not in PROCESSING_AI state. Current state: {}", matchId, match.getStatus());
            return;
        }

        if (effectiveApiKey().isEmpty()) {
            log.warn("AI API Key missing for match {}. Applying DRAW fallback.", matchId);
            applyFallback(match);
            return;
        }

        try {
            List<Argument> arguments = argumentRepository.findByMatchIdOrderByRoundNumberAsc(matchId);
            DebateTopic topic = debateTopicRepository.findByTitle(match.getTopic()).orElse(null);

            String systemPrompt = constructSystemPrompt();
            String userPrompt = constructUserPrompt(match, topic, arguments);

            EvaluationResultDto result = callGeminiApi(systemPrompt, userPrompt);
            processEvaluationResult(match, result);

        } catch (Exception e) {
            log.error("Failed to evaluate match: {}. Applying fallback DRAW.", matchId, e);
            String hint = e.getMessage() != null && e.getMessage().contains("429")
                    ? "Limită API Gemini depășită. Încearcă din nou peste câteva minute."
                    : "Eroare tehnică AI. Verifică cheia API și conexiunea.";
            applyFallback(match, hint);
        }
    }

    private String effectiveApiKey() {
        if (aiApiKey == null) {
            return "";
        }
        String trimmed = aiApiKey.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("${") || "YOUR_GEMINI_API_KEY_HERE".equals(trimmed)) {
            return "";
        }
        return trimmed;
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
                "  \"winner\": \"PRO\"|\"CONTRA\"|\"DRAW\" }";
    }

    private String constructUserPrompt(Match match, DebateTopic topic, List<Argument> arguments) {
        StringBuilder sb = new StringBuilder();
        sb.append("Temă: ").append(match.getTopic()).append("\n");
        if (topic != null) {
            sb.append("Categorie: ").append(topic.getCategory()).append("\n");
        }
        if (match.getGameMode() == GameMode.TEAMS_2V2) {
            sb.append("Format: 2v2 echipe (PRO vs CONTRA), fiecare jucător un argument.\n");
        }
        sb.append("Argumente:\n");
        for (Argument arg : arguments) {
            String role = isProSide(match, arg.getUser().getId()) ? "PRO" : "CONTRA";
            sb.append(String.format("Runda %d - %s (%s): %s\n",
                    arg.getRoundNumber(), role, arg.getUser().getUsername(), arg.getTextContent()));
        }
        return sb.toString();
    }

    private boolean isProSide(Match match, Long userId) {
        return match.getProUser().getId().equals(userId)
                || (match.getProUser2() != null && match.getProUser2().getId().equals(userId));
    }

    private EvaluationResultDto callGeminiApi(String systemPrompt, String userPrompt) throws Exception {
        String key = effectiveApiKey();
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("role", "user", "parts", List.of(
                                Map.of("text", systemPrompt + "\n\nAnalizează următorul meci:\n" + userPrompt)))
                ),
                "generationConfig", Map.of("response_mime_type", "application/json")
        );

        Exception lastError = null;
        for (String model : GEMINI_MODELS) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + key;
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    String response = restClient.post()
                            .uri(url)
                            .header("Content-Type", "application/json")
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);
                    return parseGeminiResponse(response);
                } catch (RestClientResponseException e) {
                    lastError = e;
                    log.warn("Gemini {} attempt {} failed ({}): {}", model, attempt + 1, e.getStatusCode(),
                            e.getResponseBodyAsString());
                    if (e.getStatusCode().value() == 429 && attempt < 2) {
                        try {
                            Thread.sleep(1500L * (attempt + 1));
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                        continue;
                    }
                    break;
                } catch (Exception e) {
                    lastError = e;
                    log.warn("Gemini model {} failed: {}", model, e.getMessage());
                    break;
                }
            }
        }
        throw lastError != null ? lastError : new RuntimeException("Toate modelele Gemini au eșuat");
    }

    private EvaluationResultDto parseGeminiResponse(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        if (root.has("error")) {
            throw new RuntimeException("Gemini API: " + root.path("error").path("message").asText("Unknown error"));
        }
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            throw new RuntimeException("Gemini API returned no candidates");
        }
        String jsonText = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
        jsonText = extractJson(jsonText);
        return objectMapper.readValue(jsonText, EvaluationResultDto.class);
    }

    private String extractJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        Matcher m = JSON_BLOCK.matcher(trimmed);
        if (m.find()) {
            return m.group(1).trim();
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private void processEvaluationResult(Match match, EvaluationResultDto result) {
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

        String winnerLabel = result.getWinner() != null ? result.getWinner().toUpperCase() : "DRAW";
        User winner = null;
        if ("PRO".equals(winnerLabel)) {
            winner = match.getProUser();
        } else if ("CONTRA".equals(winnerLabel)) {
            winner = match.getContraUser();
        }
        match.setWinner(winner);
        match.setStatus("FINISHED");

        updateMatchScores(match, result);

        EloResultDto eloResult = eloCalculator.calculateElo(
                match.getProUser().getEloRating(),
                match.getContraUser().getEloRating(),
                winnerLabel
        );

        updateUserElo(match, eloResult);
        matchRepository.save(match);

        sendFinishEvent(match.getId(), result);
        log.info("Finished AI evaluation for match: {} winner={}", match.getId(), winnerLabel);
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
        match.getProUser().setRankTitle(rankService.titleForElo(eloResult.getNewProRating()));
        match.getContraUser().setEloRating(eloResult.getNewContraRating());
        match.getContraUser().setRankTitle(rankService.titleForElo(eloResult.getNewContraRating()));

        userRepository.save(match.getProUser());
        userRepository.save(match.getContraUser());
    }

    private void applyFallback(Match match) {
        applyFallback(match, "Eroare tehnică AI. Meciul a fost arbitrat ca egalitate.");
    }

    private void applyFallback(Match match, String message) {
        EvaluationResultDto fallbackResult = EvaluationResultDto.builder()
                .winner("DRAW")
                .proScores(PlayerScoreDto.builder().logic(5).clarity(5).rhetoric(5).evidence(5).total(20)
                        .feedback(message).build())
                .contraScores(PlayerScoreDto.builder().logic(5).clarity(5).rhetoric(5).evidence(5).total(20)
                        .feedback(message).build())
                .fallacies(List.of())
                .build();

        match.setStatus("FINISHED");
        match.setWinner(null);
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
