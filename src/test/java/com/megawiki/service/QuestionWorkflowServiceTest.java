package com.megawiki.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.repository.InMemoryKnowledgePageRepository;
import com.megawiki.repository.InMemoryQuestionThreadRepository;
import org.junit.jupiter.api.Test;

class QuestionWorkflowServiceTest {

    private final InMemoryKnowledgePageRepository knowledgePageRepository = new InMemoryKnowledgePageRepository();
    private final InMemoryQuestionThreadRepository questionThreadRepository = new InMemoryQuestionThreadRepository();
    private final QuestionWorkflowService questionWorkflowService = new QuestionWorkflowService(
            new RuleBasedAiAnswerGenerator(),
            knowledgePageRepository,
            questionThreadRepository,
            new SlugGenerator()
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
}