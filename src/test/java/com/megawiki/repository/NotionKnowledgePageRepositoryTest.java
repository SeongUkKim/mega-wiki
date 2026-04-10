package com.megawiki.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotionKnowledgePageRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotionApiClient notionApiClient;

    private NotionKnowledgePageRepository repository;

    @BeforeEach
    void setUp() {
        NotionDataSourceRegistry registry = new NotionDataSourceRegistry();
        registry.register("pages-id", "questions-id");
        repository = new NotionKnowledgePageRepository(notionApiClient, registry, objectMapper);
    }

    @Test
    void findAllFollowsPaginationCursor() {
        when(notionApiClient.queryDataSource(eq("pages-id"), anyMap()))
                .thenReturn(pageResponse(true, "cursor-1", knowledgePageNode("page-1", "Page 1")))
                .thenReturn(pageResponse(false, null, knowledgePageNode("page-2", "Page 2")));

        assertThat(repository.findAll()).hasSize(2);

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notionApiClient, times(2)).queryDataSource(eq("pages-id"), bodyCaptor.capture());
        assertThat(bodyCaptor.getAllValues().get(1)).containsEntry("start_cursor", "cursor-1");
    }

    @Test
    void findByIdReturnsEmptyWhenPageDoesNotExist() {
        when(notionApiClient.retrievePage("missing-page"))
                .thenThrow(new NoSuchElementException("Page not found: missing-page"));

        assertThat(repository.findById("missing-page")).isEmpty();
    }

    private JsonNode pageResponse(boolean hasMore, String nextCursor, JsonNode result) {
        ObjectNode response = objectMapper.createObjectNode();
        ArrayNode results = response.putArray("results");
        results.add(result);
        response.put("has_more", hasMore);
        if (nextCursor == null) {
            response.putNull("next_cursor");
        } else {
            response.put("next_cursor", nextCursor);
        }
        return response;
    }

    private JsonNode knowledgePageNode(String id, String title) {
        return objectMapper.valueToTree(Map.of(
                "id", id,
                "created_time", "2025-01-01T00:00:00Z",
                "last_edited_time", "2025-01-02T00:00:00Z",
                "properties", Map.of(
                        "Name", Map.of("title", List.of(Map.of("plain_text", title))),
                        "Slug", Map.of("rich_text", List.of(Map.of("plain_text", "slug-" + id))),
                        "Summary", Map.of("rich_text", List.of(Map.of("plain_text", "summary"))),
                        "Content", Map.of("rich_text", List.of(Map.of("plain_text", "content"))),
                        "Tags", Map.of("multi_select", List.of(Map.of("name", "tag"))),
                        "SourceType", Map.of("select", Map.of("name", "MANUAL")),
                        "Status", Map.of("select", Map.of("name", "CURATED")),
                        "ContributionsJson", Map.of("rich_text", List.of()),
                        "LinkedQuestions", Map.of("number", 1),
                        "HelpfulCount", Map.of("number", 2)
                )
        ));
    }
}