package com.megawiki.repository;

import static com.megawiki.repository.NotionRichTextSupport.multiSelectProperty;
import static com.megawiki.repository.NotionRichTextSupport.numberProperty;
import static com.megawiki.repository.NotionRichTextSupport.parseDateTime;
import static com.megawiki.repository.NotionRichTextSupport.readEnum;
import static com.megawiki.repository.NotionRichTextSupport.readMultiSelect;
import static com.megawiki.repository.NotionRichTextSupport.readRichText;
import static com.megawiki.repository.NotionRichTextSupport.readTitle;
import static com.megawiki.repository.NotionRichTextSupport.richTextProperty;
import static com.megawiki.repository.NotionRichTextSupport.selectProperty;
import static com.megawiki.repository.NotionRichTextSupport.titleProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.megawiki.domain.Contribution;
import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.KnowledgeStatus;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
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
    private final NotionPagedQuerySupport notionPagedQuerySupport;
    private final ObjectMapper objectMapper;

    public NotionKnowledgePageRepository(
            NotionApiClient notionApiClient,
            NotionDataSourceRegistry notionDataSourceRegistry,
            NotionPagedQuerySupport notionPagedQuerySupport,
            ObjectMapper objectMapper
    ) {
        this.notionApiClient = notionApiClient;
        this.notionDataSourceRegistry = notionDataSourceRegistry;
        this.notionPagedQuerySupport = notionPagedQuerySupport;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<KnowledgePage> findAll() {
        return queryPages(null);
    }

    @Override
    public List<KnowledgePage> findRecent(int limit) {
        return queryPages(limit);
    }

    @Override
    public boolean existsAny() {
        return !queryPages(1).isEmpty();
    }

    @Override
    public Optional<KnowledgePage> findById(String id) {
        try {
            return Optional.of(mapPage(notionApiClient.retrievePage(id)));
        } catch (NoSuchElementException exception) {
            return Optional.empty();
        }
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
        return queryPages(null).size();
    }

    private List<KnowledgePage> queryPages(Integer limit) {
        return notionPagedQuerySupport.query(notionDataSourceRegistry.getPagesDataSourceId(), limit, this::mapPage);
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
}