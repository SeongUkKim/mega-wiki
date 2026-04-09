package com.megawiki.integration.slack;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SlackEventDeduplicator {

    private static final long RETENTION_SECONDS = 3600;

    private final Map<String, Instant> processedEvents = new ConcurrentHashMap<>();

    public boolean tryMarkProcessed(String eventId) {
        if (!StringUtils.hasText(eventId)) {
            return false;
        }

        Instant now = Instant.now();
        processedEvents.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minusSeconds(RETENTION_SECONDS)));
        return processedEvents.putIfAbsent(eventId, now) == null;
    }
}
