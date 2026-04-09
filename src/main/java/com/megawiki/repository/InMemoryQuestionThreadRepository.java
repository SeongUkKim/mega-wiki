package com.megawiki.repository;

import com.megawiki.domain.QuestionThread;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryQuestionThreadRepository implements QuestionThreadRepository {

    private final Map<String, QuestionThread> storage = new ConcurrentHashMap<>();

    @Override
    public List<QuestionThread> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(QuestionThread::getCreatedAt).reversed())
                .toList();
    }

    @Override
    public List<QuestionThread> findRecent(int limit) {
        return findAll().stream().limit(limit).toList();
    }

    @Override
    public Optional<QuestionThread> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public QuestionThread save(QuestionThread thread) {
        if (thread.getId() == null) {
            thread.assignId(UUID.randomUUID().toString());
        }
        storage.put(thread.getId(), thread);
        return thread;
    }

    @Override
    public long count() {
        return storage.size();
    }
}
