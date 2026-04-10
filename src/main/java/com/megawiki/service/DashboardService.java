package com.megawiki.service;

import com.megawiki.config.MegaWikiProperties;
import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.QuestionThread;
import com.megawiki.repository.DashboardQueryRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {

    private final MegaWikiProperties megaWikiProperties;
    private final DashboardQueryRepository dashboardQueryRepository;

    public DashboardService(
            MegaWikiProperties megaWikiProperties,
            DashboardQueryRepository dashboardQueryRepository
    ) {
        this.megaWikiProperties = megaWikiProperties;
        this.dashboardQueryRepository = dashboardQueryRepository;
    }

    public DashboardSnapshot loadDashboard() {
        DashboardQueryRepository.DashboardQueryResult dashboard = dashboardQueryRepository.load(6);

        return new DashboardSnapshot(
                megaWikiProperties.getTitle(),
                megaWikiProperties.getSlogan(),
                megaWikiProperties.getMissionTrack(),
                megaWikiProperties.getAiProvider(),
                dashboard.pageCount(),
                dashboard.questionCount(),
                dashboard.contributionCount(),
                dashboard.recentPages(),
                dashboard.recentQuestions(),
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