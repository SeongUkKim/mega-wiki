package com.megawiki.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.QuestionThread;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "notion")
public class NotionQuestionThreadRepository implements QuestionThreadRepository {

    private final NotionApiClient notionApiClient;
    private final NotionDataSourceRegistry notionDataSourceRegistry;

    public NotionQuestionThreadRepository(NotionApiClient notionApiClient, NotionDataSourceRegistry notionDataSourceRegistry) {
        this.notionApiClient = notionApiClient;
        this.notionDataSourceRegistry = notionDataSourceRegistry;
    }

    @Override
    public List<QuestionThread> findAll() {
        return queryThreads(100);
    }

    @Override
    public List<QuestionThread> findRecent(int limit) {
        return queryThreads(limit);
    }

    @Override
    public Optional<QuestionThread> findById(String id) {
        return Optional.of(mapThread(notionApiClient.retrievePage(id)));
    }

    @Override
    public QuestionThread save(QuestionThread thread) {
        JsonNode response;
        if (thread.getId() == null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("parent", Map.of("data_source_id", notionDataSourceRegistry.getQuestionsDataSourceId()));
            body.put("properties", buildProperties(thread));
            response = notionApiClient.createPage(body);
        } else {
            response = notionApiClient.updatePage(thread.getId(), Map.of("properties", buildProperties(thread)));
        }
        return mapThread(response);
    }

    @Override
    public long count() {
        return findAll().size();
    }

    private List<QuestionThread> queryThreads(int limit) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page_size", Math.max(limit, 1));
        body.put("sorts", List.of(Map.of(
                "timestamp", "last_edited_time",
                "direction", "descending"
        )));
        JsonNode response = notionApiClient.queryDataSource(notionDataSourceRegistry.getQuestionsDataSourceId(), body);
        return StreamSupport.stream(response.path("results").spliterator(), false)
                .map(this::mapThread)
                .limit(limit)
                .toList();
    }

    private Map<String, Object> buildProperties(QuestionThread thread) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("Name", titleProperty(trimForTitle(thread.getQuestion())));
        properties.put("Author", richTextProperty(thread.getAuthor()));
        properties.put("Channel", richTextProperty(thread.getChannel()));
        properties.put("Question", richTextProperty(thread.getQuestion()));
        properties.put("AiAnswer", richTextProperty(thread.getAiAnswer()));
        properties.put("LinkedPageId", richTextProperty(thread.getLinkedPageId()));
        properties.put("SourceType", selectProperty(thread.getSourceType().name()));
        return properties;
    }

    private QuestionThread mapThread(JsonNode pageNode) {
        JsonNode properties = pageNode.path("properties");
        return QuestionThread.restore(
                pageNode.path("id").asText(),
                readRichText(properties, "Author"),
                readRichText(properties, "Channel"),
                readRichText(properties, "Question"),
                readRichText(properties, "AiAnswer"),
                readRichText(properties, "LinkedPageId"),
                readEnum(properties, "SourceType", KnowledgeSourceType.class, KnowledgeSourceType.SLACK_THREAD),
                parseDateTime(pageNode.path("created_time").asText())
        );
    }

    private static LocalDateTime parseDateTime(String value) {
        return OffsetDateTime.parse(value).toLocalDateTime();
    }

    private static String trimForTitle(String question) {
        if (question.length() <= 60) {
            return question;
        }
        return question.substring(0, 57) + "...";
    }

    private static Map<String, Object> titleProperty(String value) {
        return Map.of("title", richTextItems(value));
    }

    private static Map<String, Object> richTextProperty(String value) {
        return Map.of("rich_text", richTextItems(value));
    }

    private static Map<String, Object> selectProperty(String value) {
        return Map.of("select", Map.of("name", value));
    }

    private static List<Map<String, Object>> richTextItems(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (String segment : splitText(value, 1800)) {
            items.add(Map.of(
                    "type", "text",
                    "text", Map.of("content", segment)
            ));
        }
        return items;
    }

    private static List<String> splitText(String value, int segmentSize) {
        List<String> segments = new ArrayList<>();
        for (int index = 0; index < value.length(); index += segmentSize) {
            segments.add(value.substring(index, Math.min(index + segmentSize, value.length())));
        }
        return segments;
    }

    private static String readRichText(JsonNode properties, String propertyName) {
        StringBuilder builder = new StringBuilder();
        properties.path(propertyName).path("rich_text")
                .forEach(item -> builder.append(item.path("plain_text").asText()));
        return builder.toString();
    }

    private static <E extends Enum<E>> E readEnum(JsonNode properties, String propertyName, Class<E> enumType, E fallback) {
        String raw = properties.path(propertyName).path("select").path("name").asText();
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Enum.valueOf(enumType, raw);
    }
}