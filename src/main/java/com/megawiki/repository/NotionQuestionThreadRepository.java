package com.megawiki.repository;

import static com.megawiki.repository.NotionRichTextSupport.parseDateTime;
import static com.megawiki.repository.NotionRichTextSupport.readEnum;
import static com.megawiki.repository.NotionRichTextSupport.readRichText;
import static com.megawiki.repository.NotionRichTextSupport.richTextProperty;
import static com.megawiki.repository.NotionRichTextSupport.selectProperty;
import static com.megawiki.repository.NotionRichTextSupport.titleProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.QuestionThread;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "notion")
public class NotionQuestionThreadRepository implements QuestionThreadRepository {

    private final NotionApiClient notionApiClient;
    private final NotionDataSourceRegistry notionDataSourceRegistry;
    private final NotionPagedQuerySupport notionPagedQuerySupport;

    public NotionQuestionThreadRepository(
            NotionApiClient notionApiClient,
            NotionDataSourceRegistry notionDataSourceRegistry,
            NotionPagedQuerySupport notionPagedQuerySupport
    ) {
        this.notionApiClient = notionApiClient;
        this.notionDataSourceRegistry = notionDataSourceRegistry;
        this.notionPagedQuerySupport = notionPagedQuerySupport;
    }

    @Override
    public List<QuestionThread> findAll() {
        return queryThreads(null);
    }

    @Override
    public List<QuestionThread> findRecent(int limit) {
        return queryThreads(limit);
    }

    @Override
    public Optional<QuestionThread> findById(String id) {
        try {
            return Optional.of(mapThread(notionApiClient.retrievePage(id)));
        } catch (NoSuchElementException exception) {
            return Optional.empty();
        }
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
        return queryThreads(null).size();
    }

    private List<QuestionThread> queryThreads(Integer limit) {
        return notionPagedQuerySupport.query(notionDataSourceRegistry.getQuestionsDataSourceId(), limit, this::mapThread);
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

    private static String trimForTitle(String question) {
        if (question.length() <= 60) {
            return question;
        }
        return question.substring(0, 57) + "...";
    }
}