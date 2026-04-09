package com.megawiki.service;

import com.megawiki.config.MegaWikiProperties;
import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.QuestionThread;
import com.megawiki.repository.KnowledgePageRepository;
import com.megawiki.repository.QuestionThreadRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final MegaWikiProperties megaWikiProperties;
    private final KnowledgePageRepository knowledgePageRepository;
    private final QuestionThreadRepository questionThreadRepository;

    public DashboardService(
            MegaWikiProperties megaWikiProperties,
            KnowledgePageRepository knowledgePageRepository,
            QuestionThreadRepository questionThreadRepository
    ) {
        this.megaWikiProperties = megaWikiProperties;
        this.knowledgePageRepository = knowledgePageRepository;
        this.questionThreadRepository = questionThreadRepository;
    }

    public DashboardSnapshot loadDashboard() {
        List<KnowledgePage> recentPages = knowledgePageRepository.findRecent(6);
        List<QuestionThread> recentQuestions = questionThreadRepository.findRecent(6);
        long contributions = knowledgePageRepository.findAll().stream()
                .mapToLong(page -> page.getContributions().size())
                .sum();

        return new DashboardSnapshot(
                megaWikiProperties.getTitle(),
                megaWikiProperties.getSlogan(),
                megaWikiProperties.getMissionTrack(),
                megaWikiProperties.getAiProvider(),
                knowledgePageRepository.count(),
                questionThreadRepository.count(),
                contributions,
                recentPages,
                recentQuestions,
                List.of(
                        "Convert repeated Slack questions into durable wiki assets",
                        "Blend AI drafts with teammate corrections in a single workflow",
                        "Keep Notion as the system of record for onboarding knowledge"
                )
        );
    }

    public record DashboardSnapshot(
            String title,
            String slogan,
            String missionTrack,
            String aiProvider,
            long pageCount,
            long questionCount,
            long contributionCount,
            List<KnowledgePage> recentPages,
            List<QuestionThread> recentQuestions,
            List<String> differentiators
    ) {
    }
}