package com.megawiki.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.megawiki.domain.Contribution;
import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.KnowledgeStatus;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "notion")
public class NotionKnowledgePageRepository implements KnowledgePageRepository {

    private static final TypeReference<List<Contribution>> CONTRIBUTION_LIST_TYPE = new TypeReference<>() {
    };

    private final NotionApiClient notionApiClient;
    private final NotionDataSourceRegistry notionDataSourceRegistry;
    private final ObjectMapper objectMapper;

    public NotionKnowledgePageRepository(
            NotionApiClient notionApiClient,
            NotionDataSourceRegistry notionDataSourceRegistry,
            ObjectMapper objectMapper
    ) {
        this.notionApiClient = notionApiClient;
        this.notionDataSourceRegistry = notionDataSourceRegistry;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<KnowledgePage> findAll() {
        return queryPages(100);
    }

    @Override
    public List<KnowledgePage> findRecent(int limit) {
        return queryPages(limit);
    }

    @Override
    public Optional<KnowledgePage> findById(String id) {
        return Optional.of(mapPage(notionApiClient.retrievePage(id)));
    }

    @Override
    public Optional<KnowledgePage> findBySlug(String slug) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page_size", 1);
        body.put("filter", Map.of(
                "property", "Slug",
                "rich_text", Map.of("equals", slug)
        ));
        JsonNode response = notionApiClient.queryDataSource(notionDataSourceRegistry.getPagesDataSourceId(), body);
        return StreamSupport.stream(response.path("results").spliterator(), false)
                .findFirst()
                .map(this::mapPage);
    }

    @Override
    public KnowledgePage save(KnowledgePage page) {
        JsonNode response;
        if (page.getId() == null) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("parent", Map.of("data_source_id", notionDataSourceRegistry.getPagesDataSourceId()));
            body.put("properties", buildProperties(page));
            response = notionApiClient.createPage(body);
        } else {
            response = notionApiClient.updatePage(page.getId(), Map.of("properties", buildProperties(page)));
        }
        return mapPage(response);
    }

    @Override
    public long count() {
        return findAll().size();
    }

    private List<KnowledgePage> queryPages(int limit) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("page_size", Math.max(limit, 1));
        body.put("sorts", List.of(Map.of(
                "timestamp", "last_edited_time",
                "direction", "descending"
        )));
        JsonNode response = notionApiClient.queryDataSource(notionDataSourceRegistry.getPagesDataSourceId(), body);
        return StreamSupport.stream(response.path("results").spliterator(), false)
                .map(this::mapPage)
                .limit(limit)
                .toList();
    }

    private Map<String, Object> buildProperties(KnowledgePage page) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("Name", titleProperty(page.getTitle()));
        properties.put("Slug", richTextProperty(page.getSlug()));
        properties.put("Summary", richTextProperty(page.getSummary()));
        properties.put("Content", richTextProperty(page.getContent()));
        properties.put("ContributionsJson", richTextProperty(serializeContributions(page.getContributions())));
        properties.put("SourceType", selectProperty(page.getSourceType().name()));
        properties.put("Status", selectProperty(page.getStatus().name()));
        properties.put("Tags", multiSelectProperty(page.getTags()));
        properties.put("LinkedQuestions", numberProperty(page.getLinkedQuestionCount()));
        properties.put("HelpfulCount", numberProperty(page.getHelpfulCount()));
        return properties;
    }

    private KnowledgePage mapPage(JsonNode pageNode) {
        JsonNode properties = pageNode.path("properties");
        return KnowledgePage.restore(
                pageNode.path("id").asText(),
                readTitle(properties, "Name"),
                readRichText(properties, "Slug"),
                readRichText(properties, "Summary"),
                readRichText(properties, "Content"),
                readMultiSelect(properties, "Tags"),
                readEnum(properties, "SourceType", KnowledgeSourceType.class, KnowledgeSourceType.MANUAL),
                readEnum(properties, "Status", KnowledgeStatus.class, KnowledgeStatus.DRAFT),
                deserializeContributions(readRichText(properties, "ContributionsJson")),
                properties.path("LinkedQuestions").path("number").asInt(0),
                properties.path("HelpfulCount").path("number").asInt(0),
                parseDateTime(pageNode.path("created_time").asText()),
                parseDateTime(pageNode.path("last_edited_time").asText())
        );
    }

    private String serializeContributions(List<Contribution> contributions) {
        try {
            return objectMapper.writeValueAsString(contributions);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize contributions", exception);
        }
    }

    private List<Contribution> deserializeContributions(String raw) {
        if (raw == null || raw.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(raw, CONTRIBUTION_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to deserialize contributions", exception);
        }
    }

    private static LocalDateTime parseDateTime(String value) {
        return OffsetDateTime.parse(value).toLocalDateTime();
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

    private static Map<String, Object> numberProperty(int value) {
        return Map.of("number", value);
    }

    private static Map<String, Object> multiSelectProperty(Set<String> values) {
        return Map.of("multi_select", values.stream().map(tag -> Map.of("name", tag)).toList());
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

    private static String readTitle(JsonNode properties, String propertyName) {
        return joinTexts(properties.path(propertyName).path("title"));
    }

    private static String readRichText(JsonNode properties, String propertyName) {
        return joinTexts(properties.path(propertyName).path("rich_text"));
    }

    private static String joinTexts(JsonNode arrayNode) {
        StringBuilder builder = new StringBuilder();
        arrayNode.forEach(item -> builder.append(item.path("plain_text").asText()));
        return builder.toString();
    }

    private static Set<String> readMultiSelect(JsonNode properties, String propertyName) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        properties.path(propertyName).path("multi_select")
                .forEach(option -> tags.add(option.path("name").asText()));
        return tags;
    }

    private static <E extends Enum<E>> E readEnum(JsonNode properties, String propertyName, Class<E> enumType, E fallback) {
        String raw = properties.path(propertyName).path("select").path("name").asText();
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Enum.valueOf(enumType, raw);
    }
}
