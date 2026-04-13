package com.megawiki.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.KnowledgeStatus;
import com.megawiki.repository.InMemoryKnowledgePageRepository;
import com.megawiki.repository.InMemoryQuestionThreadRepository;
import java.util.Set;
import org.junit.jupiter.api.Test;

class QuestionWorkflowServiceTest {

    private final InMemoryKnowledgePageRepository knowledgePageRepository = new InMemoryKnowledgePageRepository();
    private final InMemoryQuestionThreadRepository questionThreadRepository = new InMemoryQuestionThreadRepository();
    private final QuestionWorkflowService questionWorkflowService = new QuestionWorkflowService(
            new RuleBasedAiAnswerGenerator(),
            knowledgePageRepository,
            questionThreadRepository,
            new SlugGenerator(),
            new KnowledgeContextService(knowledgePageRepository)
    );

    @Test
    void submitReturnsSavedPageAndQuestionThread() {
        QuestionSubmissionResult result = questionWorkflowService.submit(new QuestionSubmissionCommand(
                "new-hire",
                "#general",
                "Where is the remote work request guide?",
                KnowledgeSourceType.SLACK_THREAD
        ));

        assertThat(result.page().getId()).isNotBlank();
        assertThat(result.thread().getId()).isNotBlank();
        assertThat(result.thread().getLinkedPageId()).isEqualTo(result.page().getId());
    }

    @Test
    void submitQuestionCreatesPageAndQuestionThread() {
        var thread = questionWorkflowService.submitQuestion(
                "new-hire",
                "#general",
                "Where is the remote work request guide?",
                KnowledgeSourceType.SLACK_THREAD
        );

        assertThat(thread.getId()).isNotBlank();
        assertThat(thread.getLinkedPageId()).isNotBlank();
        assertThat(knowledgePageRepository.findAll()).hasSize(1);
        assertThat(questionThreadRepository.findAll()).hasSize(1);
        assertThat(knowledgePageRepository.findAll().get(0).getTitle()).isEqualTo("Remote Work Guide");
        assertThat(knowledgePageRepository.findAll().get(0).getLinkedQuestionCount()).isEqualTo(1);
    }

    @Test
    void submitQuestionReusesExistingPageForSameTopic() {
        questionWorkflowService.submitQuestion("A", "#general", "I need the remote work guide.", KnowledgeSourceType.SLACK_THREAD);
        questionWorkflowService.submitQuestion("B", "#onboarding", "What is the remote work request path?", KnowledgeSourceType.SLACK_THREAD);

        assertThat(knowledgePageRepository.findAll()).hasSize(1);
        assertThat(knowledgePageRepository.findAll().get(0).getLinkedQuestionCount()).isEqualTo(2);
        assertThat(questionThreadRepository.findAll()).hasSize(2);
    }

    @Test
    void submitUsesExistingRegisteredPageBeforeAiGeneration() {
        KnowledgePage existingPage = KnowledgePage.create(
                "\uC5F0\uCC28 \uC0AC\uC6A9 \uC2E0\uCCAD \uBC29\uBC95",
                "annual-leave-policy",
                "\uC5F0\uCC28 \uC2E0\uCCAD\uC740 HR \uD3EC\uD138\uC5D0\uC11C \uC9C4\uD589\uD569\uB2C8\uB2E4.",
                "\uC5F0\uCC28 \uC0AC\uC6A9 \uC2E0\uCCAD\uC740 \uB0B4\uBD80 HR \uD3EC\uD138\uC744 \uD1B5\uD574 \uC9C4\uD589\uB429\uB2C8\uB2E4. \uD3EC\uD138\uC5D0 \uC811\uC18D\uD558\uC5EC \uC5F0\uCC28 \uC2E0\uCCAD \uBA54\uB274\uC5D0\uC11C \uD544\uC694\uD55C \uC815\uBCF4\uB97C \uC785\uB825\uD558\uACE0 \uC2E0\uCCAD\uC744 \uC644\uB8CC\uD558\uC138\uC694.",
                Set.of("hr", "leave"),
                KnowledgeSourceType.MANUAL,
                KnowledgeStatus.CURATED
        );
        knowledgePageRepository.save(existingPage);

        QuestionWorkflowService service = new QuestionWorkflowService(
                (question, sourceType) -> {
                    throw new IllegalStateException("AI should not be called when a registered page already matches.");
                },
                knowledgePageRepository,
                questionThreadRepository,
                new SlugGenerator(),
                new KnowledgeContextService(knowledgePageRepository)
        );

        QuestionSubmissionResult result = service.submit(new QuestionSubmissionCommand(
                "employee",
                "#hr",
                "\uC5F0\uCC28 \uC0AC\uC6A9",
                KnowledgeSourceType.SLACK_THREAD
        ));

        assertThat(result.reusedExistingPage()).isTrue();
        assertThat(result.page().getId()).isEqualTo(existingPage.getId());
        assertThat(result.thread().getAiAnswer()).contains("HR \uD3EC\uD138");
        assertThat(result.page().getLinkedQuestionCount()).isEqualTo(1);
        assertThat(knowledgePageRepository.findAll()).hasSize(1);
    }

    @Test
    void submitMatchesExistingPageWhenQuestionUsesSynonymsAndParticles() {
        String title = "\uC9C0\uC2DD \uBB38\uC11C \uB0B4 \uB2F5\uBCC0 \uC911\uBCF5 \uC6D0\uC778";
        String cannedAnswer = "fallback \uD14C\uC2A4\uD2B8 \uB2F5\uBCC0\uC785\uB2C8\uB2E4";
        KnowledgePage existingPage = KnowledgePage.create(
                title,
                "duplicate-answer-cause",
                cannedAnswer,
                cannedAnswer,
                Set.of("slack", "faq"),
                KnowledgeSourceType.MANUAL,
                KnowledgeStatus.CURATED
        );
        knowledgePageRepository.save(existingPage);

        QuestionWorkflowService service = new QuestionWorkflowService(
                (question, sourceType) -> {
                    throw new IllegalStateException("AI should not be called when synonym matching finds a registered page.");
                },
                knowledgePageRepository,
                questionThreadRepository,
                new SlugGenerator(),
                new KnowledgeContextService(knowledgePageRepository)
        );

        QuestionSubmissionResult result = service.submit(new QuestionSubmissionCommand(
                "employee",
                "#slack-help",
                "\uB2F5\uBCC0\uC774 \uB450\uBC88 \uC624\uB294 \uC774\uC720",
                KnowledgeSourceType.SLACK_THREAD
        ));

        assertThat(result.reusedExistingPage()).isTrue();
        assertThat(result.page().getId()).isEqualTo(existingPage.getId());
        assertThat(result.thread().getAiAnswer()).isEqualTo(cannedAnswer);
        assertThat(knowledgePageRepository.findAll()).hasSize(1);
    }

    @Test
    void submitCreatesQuestionSpecificFallbackPageInsteadOfMergingGenericPage() {
        KnowledgePage genericPage = KnowledgePage.create(
                "General Onboarding Guide",
                "general-onboarding-guide",
                "Guide",
                "Generic content",
                Set.of("onboarding"),
                KnowledgeSourceType.MANUAL,
                KnowledgeStatus.DRAFT
        );
        knowledgePageRepository.save(genericPage);

        QuestionSubmissionResult result = questionWorkflowService.submit(new QuestionSubmissionCommand(
                "employee",
                "#general",
                "Why does the answer arrive twice?",
                KnowledgeSourceType.SLACK_THREAD
        ));

        assertThat(result.reusedExistingPage()).isFalse();
        assertThat(result.page().getId()).isNotEqualTo(genericPage.getId());
        assertThat(result.page().getTitle()).isEqualTo("Why does the answer arrive twice?");
        assertThat(knowledgePageRepository.findAll()).hasSize(2);
    }
}