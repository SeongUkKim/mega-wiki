package com.megawiki.repository;

import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.QuestionThread;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "memory", matchIfMissing = true)
public class InMemoryDashboardQueryRepository implements DashboardQueryRepository {

    private final KnowledgePageRepository knowledgePageRepository;
    private final QuestionThreadRepository questionThreadRepository;

    public InMemoryDashboardQueryRepository(
            KnowledgePageRepository knowledgePageRepository,
            QuestionThreadRepository questionThreadRepository
    ) {
        this.knowledgePageRepository = knowledgePageRepository;
        this.questionThreadRepository = questionThreadRepository;
    }

    @Override
    public DashboardQueryResult load(int recentLimit) {
        List<KnowledgePage> pages = knowledgePageRepository.findAll();
        List<QuestionThread> questions = questionThreadRepository.findAll();
        long contributionCount = pages.stream()
                .mapToLong(page -> page.getContributions().size())
                .sum();

        return new DashboardQueryResult(
                pages.size(),
                questions.size(),
                contributionCount,
                pages.stream().limit(recentLimit).toList(),
                questions.stream().limit(recentLimit).toList()
        );
    }
}