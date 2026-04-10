package com.megawiki.repository;

import com.megawiki.domain.KnowledgePage;
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
public class InMemoryKnowledgePageRepository implements KnowledgePageRepository {

    private final Map<String, KnowledgePage> storage = new ConcurrentHashMap<>();

    @Override
    public List<KnowledgePage> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(KnowledgePage::getUpdatedAt).reversed())
                .toList();
    }

    @Override
    public List<KnowledgePage> findRecent(int limit) {
        return findAll().stream().limit(limit).toList();
    }

    @Override
    public boolean existsAny() {
        return !storage.isEmpty();
    }

    @Override
    public Optional<KnowledgePage> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<KnowledgePage> findBySlug(String slug) {
        return storage.values().stream()
                .filter(page -> page.getSlug().equals(slug))
                .findFirst();
    }

    @Override
    public KnowledgePage save(KnowledgePage page) {
        if (page.getId() == null) {
            page.assignId(UUID.randomUUID().toString());
        }
        storage.put(page.getId(), page);
        return page;
    }

    @Override
    public long count() {
        return storage.size();
    }
}