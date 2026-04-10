package com.megawiki.repository;

import com.megawiki.domain.KnowledgePage;
import java.util.List;
import java.util.Optional;

public interface KnowledgePageRepository {

    List<KnowledgePage> findAll();

    List<KnowledgePage> findRecent(int limit);

    boolean existsAny();

    Optional<KnowledgePage> findById(String id);

    Optional<KnowledgePage> findBySlug(String slug);

    KnowledgePage save(KnowledgePage page);

    long count();
}