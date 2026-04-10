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
class NotionQuestionThreadRepositoryTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private NotionApiClient notionApiClient;

    private NotionQuestionThreadRepository repository;

    @BeforeEach
    void setUp() {
        NotionDataSourceRegistry registry = new NotionDataSourceRegistry();
        registry.register("pages-id", "questions-id");
        repository = new NotionQuestionThreadRepository(
                notionApiClient,
                registry,
                new NotionPagedQuerySupport(notionApiClient)
        );
    }

    @Test
    void findAllFollowsPaginationCursor() {
        when(notionApiClient.queryDataSource(eq("questions-id"), anyMap()))
                .thenReturn(pageResponse(true, "cursor-1", questionThreadNode("thread-1", "Question 1")))
                .thenReturn(pageResponse(false, null, questionThreadNode("thread-2", "Question 2")));

        assertThat(repository.findAll()).hasSize(2);

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notionApiClient, times(2)).queryDataSource(eq("questions-id"), bodyCaptor.capture());
        assertThat(bodyCaptor.getAllValues().get(1)).containsEntry("start_cursor", "cursor-1");
    }

    @Test
    void findByIdReturnsEmptyWhenThreadDoesNotExist() {
        when(notionApiClient.retrievePage("missing-thread"))
                .thenThrow(new NoSuchElementException("Page not found: missing-thread"));

        assertThat(repository.findById("missing-thread")).isEmpty();
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

    private JsonNode questionThreadNode(String id, String question) {
        return objectMapper.valueToTree(Map.of(
                "id", id,
                "created_time", "2025-01-01T00:00:00Z",
                "properties", Map.of(
                        "Author", Map.of("rich_text", List.of(Map.of("plain_text", "author"))),
                        "Channel", Map.of("rich_text", List.of(Map.of("plain_text", "channel"))),
                        "Question", Map.of("rich_text", List.of(Map.of("plain_text", question))),
                        "AiAnswer", Map.of("rich_text", List.of(Map.of("plain_text", "answer"))),
                        "LinkedPageId", Map.of("rich_text", List.of(Map.of("plain_text", "page-1"))),
                        "SourceType", Map.of("select", Map.of("name", "SLACK_THREAD"))
                )
        ));
    }
}