package com.megawiki.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.megawiki.config.GeminiProperties;
import com.megawiki.domain.KnowledgeSourceType;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
@Primary
public class GeminiAiAnswerGenerator implements AiAnswerGenerator {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiAnswerGenerator.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final GeminiProperties geminiProperties;
    private final KnowledgeContextService knowledgeContextService;
    private final RuleBasedAiAnswerGenerator fallbackGenerator;

    public GeminiAiAnswerGenerator(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            GeminiProperties geminiProperties,
            KnowledgeContextService knowledgeContextService,
            RuleBasedAiAnswerGenerator fallbackGenerator
    ) {
        this.objectMapper = objectMapper;
        this.geminiProperties = geminiProperties;
        this.knowledgeContextService = knowledgeContextService;
        this.fallbackGenerator = fallbackGenerator;
        this.restClient = restClientBuilder
                .baseUrl("https://generativelanguage.googleapis.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public AiAnswerDraft generate(String question, KnowledgeSourceType sourceType) {
        if (!geminiProperties.isEnabled() || !StringUtils.hasText(geminiProperties.getApiKey())) {
            return fallbackGenerator.generate(question, sourceType);
        }

        try {
            JsonNode response = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1beta/models/{model}:generateContent")
                            .queryParam("key", geminiProperties.getApiKey())
                            .build(geminiProperties.getModel()))
                    .body(buildRequest(question, sourceType))
                    .retrieve()
                    .body(JsonNode.class);
            return parseDraft(response, sourceType);
        } catch (RuntimeException exception) {
            log.warn("Gemini answer generation failed. Falling back to rule-based draft.", exception);
            return fallbackGenerator.generate(question, sourceType);
        }
    }

    private Map<String, Object> buildRequest(String question, KnowledgeSourceType sourceType) {
        String context = knowledgeContextService.buildContext(question);
        String prompt = "Question source: " + sourceType.name() + "\n"
                + "Question: " + question + "\n\n"
                + "Existing knowledge context:\n" + context + "\n\n"
                + "Return only JSON with this schema: "
                + "{\"title\":string,\"summary\":string,\"answer\":string,\"content\":string,\"tags\":string[]}.\n"
                + "Rules: title under 60 chars, summary 1-2 sentences, answer concise for Slack, content in markdown, tags 3-5 items, and match the user's language.";

        return Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of(
                                "text", "You are drafting reusable internal knowledge-base answers. Prefer facts from the provided context. If the context is weak, state assumptions explicitly instead of inventing policy."
                        ))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt))
                )),
                "generationConfig", Map.of(
                        "temperature", geminiProperties.getTemperature(),
                        "responseMimeType", "application/json"
                )
        );
    }

    private AiAnswerDraft parseDraft(JsonNode response, KnowledgeSourceType sourceType) {
        String rawText = response.path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text").asText();
        if (!StringUtils.hasText(rawText)) {
            throw new IllegalStateException("Gemini response did not contain draft text.");
        }

        try {
            JsonNode json = objectMapper.readTree(stripCodeFence(rawText));
            String title = readRequiredText(json, "title");
            String summary = readRequiredText(json, "summary");
            String answer = readRequiredText(json, "answer");
            String content = readRequiredText(json, "content");
            Set<String> tags = readTags(json.path("tags"), sourceType);
            return new AiAnswerDraft(title, summary, answer, content, tags);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to parse Gemini JSON response", exception);
        }
    }

    private static String stripCodeFence(String rawText) {
        String normalized = rawText.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```[a-zA-Z]*\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }
        return normalized.trim();
    }

    private static String readRequiredText(JsonNode json, String field) {
        String value = json.path(field).asText("").trim();
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("Gemini response field is blank: " + field);
        }
        return value;
    }

    private static Set<String> readTags(JsonNode tagsNode, KnowledgeSourceType sourceType) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        tagsNode.forEach(item -> {
            String value = item.asText("").trim().toLowerCase(Locale.ROOT);
            if (StringUtils.hasText(value)) {
                tags.add(value);
            }
        });
        if (!tags.isEmpty()) {
            return tags;
        }

        return switch (sourceType) {
            case SLACK_THREAD -> Set.of("slack", "onboarding", "team-knowledge");
            case MEGAONE_ASK -> Set.of("ask", "faq", "knowledge-share");
            case MANUAL -> Set.of("manual", "wiki", "knowledge-share");
        };
    }
}
