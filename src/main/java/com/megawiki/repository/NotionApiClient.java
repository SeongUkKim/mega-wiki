package com.megawiki.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.megawiki.config.NotionProperties;
import jakarta.annotation.PostConstruct;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "notion")
public class NotionApiClient {

    private final RestClient restClient;
    private final NotionProperties notionProperties;

    public NotionApiClient(RestClient.Builder restClientBuilder, NotionProperties notionProperties) {
        this.notionProperties = notionProperties;
        this.restClient = restClientBuilder
                .baseUrl("https://api.notion.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + notionProperties.getApiToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Notion-Version", notionProperties.getApiVersion())
                .build();
    }

    @PostConstruct
    void validateConfiguration() {
        if (!StringUtils.hasText(notionProperties.getApiToken())
                || !StringUtils.hasText(notionProperties.getPagesDataSourceId())
                || !StringUtils.hasText(notionProperties.getQuestionsDataSourceId())) {
            throw new IllegalStateException("Notion storage is enabled but Notion token/data source ids are missing.");
        }
    }

    public JsonNode queryDataSource(String dataSourceId, Map<String, Object> body) {
        return execute(() -> restClient.post()
                .uri("/data_sources/{id}/query", dataSourceId)
                .body(body)
                .retrieve()
                .body(JsonNode.class), "query data source " + dataSourceId);
    }

    public JsonNode retrievePage(String pageId) {
        return execute(() -> restClient.get()
                .uri("/pages/{id}", pageId)
                .retrieve()
                .body(JsonNode.class), "retrieve page " + pageId);
    }

    public JsonNode createPage(Map<String, Object> body) {
        return execute(() -> restClient.post()
                .uri("/pages")
                .body(body)
                .retrieve()
                .body(JsonNode.class), "create page");
    }

    public JsonNode updatePage(String pageId, Map<String, Object> body) {
        return execute(() -> restClient.patch()
                .uri("/pages/{id}", pageId)
                .body(body)
                .retrieve()
                .body(JsonNode.class), "update page " + pageId);
    }

    private JsonNode execute(SupplierWithException<JsonNode> action, String label) {
        try {
            return action.get();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to " + label + " via Notion API", exception);
        }
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws RestClientException;
    }
}