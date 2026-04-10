package com.megawiki.repository;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.StreamSupport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "notion")
public class NotionPagedQuerySupport {

    private final NotionApiClient notionApiClient;

    public NotionPagedQuerySupport(NotionApiClient notionApiClient) {
        this.notionApiClient = notionApiClient;
    }

    public <T> List<T> query(String dataSourceId, Integer limit, Function<JsonNode, T> mapper) {
        List<T> items = new ArrayList<>();
        String cursor = null;

        do {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("page_size", pageSize(limit, items.size()));
            body.put("sorts", List.of(Map.of(
                    "timestamp", "last_edited_time",
                    "direction", "descending"
            )));
            if (StringUtils.hasText(cursor)) {
                body.put("start_cursor", cursor);
            }

            JsonNode response = notionApiClient.queryDataSource(dataSourceId, body);
            StreamSupport.stream(response.path("results").spliterator(), false)
                    .map(mapper)
                    .limit(remaining(limit, items.size()))
                    .forEach(items::add);

            if (!response.path("has_more").asBoolean(false) || reachedLimit(limit, items.size())) {
                break;
            }
            cursor = response.path("next_cursor").asText();
        } while (StringUtils.hasText(cursor));

        return items;
    }

    private static int pageSize(Integer limit, int currentSize) {
        if (limit == null) {
            return 100;
        }
        return Math.max(Math.min(limit - currentSize, 100), 1);
    }

    private static long remaining(Integer limit, int currentSize) {
        if (limit == null) {
            return Long.MAX_VALUE;
        }
        return Math.max(limit - currentSize, 0);
    }

    private static boolean reachedLimit(Integer limit, int currentSize) {
        return limit != null && currentSize >= limit;
    }
}