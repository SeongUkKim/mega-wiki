package com.megawiki.repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.megawiki.config.NotionProperties;
import jakarta.annotation.PostConstruct;
import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
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
                .requestFactory(new JdkClientHttpRequestFactory(HttpClient.newHttpClient()))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + notionProperties.getApiToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Notion-Version", notionProperties.getApiVersion())
                .build();
    }

    @PostConstruct
    void validateConfiguration() {
        if (!StringUtils.hasText(notionProperties.getApiToken())) {
            throw new IllegalStateException("Notion storage is enabled but the Notion API token is missing.");
        }
    }

    public List<DataSourceSummary> searchDataSources(String query) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("page_size", 50);
        body.put("filter", Map.of(
                "value", "data_source",
                "property", "object"
        ));

        JsonNode response = execute(() -> restClient.post()
                .uri("/search")
                .body(body)
                .retrieve()
                .body(JsonNode.class), "search Notion data sources");

        return StreamSupport.stream(response.path("results").spliterator(), false)
                .filter(node -> "data_source".equals(node.path("object").asText()))
                .map(node -> new DataSourceSummary(
                        node.path("id").asText(),
                        joinTexts(node.path("title"))
                ))
                .toList();
    }

    public JsonNode queryDataSource(String dataSourceId, Map<String, Object> body) {
        return execute(() -> restClient.post()
                .uri("/data_sources/{id}/query", dataSourceId)
                .body(body)
                .retrieve()
                .body(JsonNode.class), "query data source " + dataSourceId);
    }

    public JsonNode updateDataSource(String dataSourceId, Map<String, Object> body) {
        return execute(() -> restClient.patch()
                .uri("/data_sources/{id}", dataSourceId)
                .body(body)
                .retrieve()
                .body(JsonNode.class), "update data source " + dataSourceId);
    }

    public JsonNode retrievePage(String pageId) {
        try {
            return restClient.get()
                    .uri("/pages/{id}", pageId)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException.NotFound exception) {
            throw new NoSuchElementException("Page not found: " + pageId);
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to retrieve page " + pageId + " via Notion API", exception);
        }
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

    private static String joinTexts(JsonNode arrayNode) {
        StringBuilder builder = new StringBuilder();
        arrayNode.forEach(item -> builder.append(item.path("plain_text").asText()));
        return builder.toString();
    }

    public record DataSourceSummary(String id, String title) {
    }

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws RestClientException;
    }
}