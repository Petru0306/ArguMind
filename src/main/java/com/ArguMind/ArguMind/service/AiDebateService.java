package com.ArguMind.ArguMind.service;

import com.ArguMind.ArguMind.dto.AiChatMessageDto;
import com.ArguMind.ArguMind.model.AiCoachPersonality;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiDebateService {

    private static final List<String> GEMINI_MODELS = List.of(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-2.5-flash-lite"
    );

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().build();

    @Value("${ai.api.key:}")
    private String aiApiKey;

    public String chat(String topic, String userMessage, List<AiChatMessageDto> history, String personality) {
        if (effectiveApiKey().isEmpty()) {
            return "Configurarea AI lipsește. Adaugă cheia Gemini în application-secrets.properties.";
        }

        String systemPrompt = buildCoachPrompt(topic, AiCoachPersonality.fromString(personality));
        List<Map<String, Object>> contents = new ArrayList<>();

        for (AiChatMessageDto turn : history) {
            if (turn.getContent() == null || turn.getContent().isBlank()) {
                continue;
            }
            String role = "assistant".equalsIgnoreCase(turn.getRole()) ? "model" : "user";
            contents.add(Map.of(
                    "role", role,
                    "parts", List.of(Map.of("text", turn.getContent()))
            ));
        }
        contents.add(Map.of("role", "user", "parts", List.of(Map.of("text", userMessage))));

        Map<String, Object> requestBody = Map.of(
                "systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))),
                "contents", contents
        );

        return callGemini(requestBody);
    }

    private String buildCoachPrompt(String topic, AiCoachPersonality personality) {
        return """
                Ești ArguMind Coach — mentor de argumentare și debate în limba română.
                Tema de antrenament: %s

                Personalitate coach (respect-o strict): %s — %s

                Rolul tău:
                - Discuți cu utilizatorul ca într-un sparring de idei (PRO sau CONTRA, sau neutru dacă cere).
                - Îl antrenezi: structură (premisă → argument → dovadă → concluzie), claritate, contra-argumente.
                - Identifici pe scurt erori logice (ad hominem, generalizări, false dichotomy) când apar.
                - Răspunsuri de 2–5 paragrafe, concrete, fără romane.
                - Nu da verdict de „câștigător meci”; e antrenament, nu arbitraj oficial.
                - Încurajează utilizatorul să formuleze argumente complete, nu doar opinii scurte.
                """.formatted(
                topic != null && !topic.isBlank() ? topic : "Dezbatere generală",
                personality.getLabel(),
                personality.getDescription());
    }

    private String callGemini(Map<String, Object> requestBody) {
        String key = effectiveApiKey();
        Exception last = null;
        for (String model : GEMINI_MODELS) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent?key=" + key;
            for (int attempt = 0; attempt < 2; attempt++) {
                try {
                    String response = restClient.post()
                            .uri(url)
                            .header("Content-Type", "application/json")
                            .body(requestBody)
                            .retrieve()
                            .body(String.class);
                    return extractText(response);
                } catch (RestClientResponseException e) {
                    last = e;
                    if (e.getStatusCode().value() == 429 && attempt == 0) {
                        try {
                            Thread.sleep(1200);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                    log.warn("AI debate {} failed: {}", model, e.getResponseBodyAsString());
                    break;
                } catch (Exception e) {
                    last = e;
                    break;
                }
            }
        }
        log.error("AI debate chat failed", last);
        return "Nu am putut răspunde acum (limită API sau eroare temporară). Încearcă din nou în câteva secunde.";
    }

    private String extractText(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        if (root.has("error")) {
            throw new RuntimeException(root.path("error").path("message").asText("Gemini error"));
        }
        return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
    }

    private String effectiveApiKey() {
        if (aiApiKey == null) {
            return "";
        }
        String t = aiApiKey.trim();
        return t.isEmpty() || t.startsWith("${") ? "" : t;
    }
}
