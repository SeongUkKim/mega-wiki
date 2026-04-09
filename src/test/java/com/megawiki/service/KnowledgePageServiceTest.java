package com.megawiki.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.megawiki.domain.KnowledgeStatus;
import com.megawiki.repository.InMemoryKnowledgePageRepository;
import org.junit.jupiter.api.Test;

class KnowledgePageServiceTest {

    private final InMemoryKnowledgePageRepository knowledgePageRepository = new InMemoryKnowledgePageRepository();
    private final KnowledgePageService knowledgePageService = new KnowledgePageService(
            knowledgePageRepository,
            new SlugGenerator()
    );

    @Test
    void createManualPageStoresCuratedDocument() {
        var page = knowledgePageService.createManualPage(
                "Onboarding Checklist",
                "Summarize the first-week setup items.",
                "Receive the laptop, activate accounts, and register the first team schedule.",
                "onboarding, checklist"
        );

        assertThat(page.getId()).isNotBlank();
        assertThat(page.getStatus()).isEqualTo(KnowledgeStatus.CURATED);
        assertThat(page.getTags()).contains("onboarding", "checklist");
        assertThat(page.getContributions()).hasSize(1);
    }

    @Test
    void addContributionAndHelpfulUpdateDocumentState() {
        var page = knowledgePageService.createManualPage(
                "Onboarding Checklist",
                "First-week onboarding guide",
                "Initial setup steps",
                "onboarding"
        );

        knowledgePageService.addContribution(page.getId(), "mentor", "Add the team wiki link as well.");
        knowledgePageService.markHelpful(page.getId());

        var storedPage = knowledgePageService.getPage(page.getId());
        assertThat(storedPage.getContributions()).hasSize(2);
        assertThat(storedPage.getHelpfulCount()).isEqualTo(1);
    }
}