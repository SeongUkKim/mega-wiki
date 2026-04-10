package com.megawiki.repository;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class NotionRichTextSupport {

    private NotionRichTextSupport() {
    }

    static LocalDateTime parseDateTime(String value) {
        return OffsetDateTime.parse(value).toLocalDateTime();
    }

    static Map<String, Object> titleProperty(String value) {
        return Map.of("title", richTextItems(value));
    }

    static Map<String, Object> richTextProperty(String value) {
        return Map.of("rich_text", richTextItems(value));
    }

    static Map<String, Object> selectProperty(String value) {
        return Map.of("select", Map.of("name", value));
    }

    static Map<String, Object> numberProperty(int value) {
        return Map.of("number", value);
    }

    static Map<String, Object> multiSelectProperty(Set<String> values) {
        return Map.of("multi_select", values.stream().map(tag -> Map.of("name", tag)).toList());
    }

    static String readTitle(JsonNode properties, String propertyName) {
        return joinTexts(properties.path(propertyName).path("title"));
    }

    static String readRichText(JsonNode properties, String propertyName) {
        return joinTexts(properties.path(propertyName).path("rich_text"));
    }

    static Set<String> readMultiSelect(JsonNode properties, String propertyName) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        properties.path(propertyName).path("multi_select")
                .forEach(option -> tags.add(option.path("name").asText()));
        return tags;
    }

    static <E extends Enum<E>> E readEnum(JsonNode properties, String propertyName, Class<E> enumType, E fallback) {
        String raw = properties.path(propertyName).path("select").path("name").asText();
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Enum.valueOf(enumType, raw);
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

    private static String joinTexts(JsonNode arrayNode) {
        StringBuilder builder = new StringBuilder();
        arrayNode.forEach(item -> builder.append(item.path("plain_text").asText()));
        return builder.toString();
    }
}