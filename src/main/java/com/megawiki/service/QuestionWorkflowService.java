package com.megawiki.service;

import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.KnowledgeStatus;
import com.megawiki.domain.QuestionThread;
import com.megawiki.repository.KnowledgePageRepository;
import com.megawiki.repository.QuestionThreadRepository;
import org.springframework.stereotype.Service;

@Service
public class QuestionWorkflowService {

    private final AiAnswerGenerator aiAnswerGenerator;
    private final KnowledgePageRepository knowledgePageRepository;
    private final QuestionThreadRepository questionThreadRepository;
    private final SlugGenerator slugGenerator;
    private final KnowledgeContextService knowledgeContextService;

    public QuestionWorkflowService(
            AiAnswerGenerator aiAnswerGenerator,
            KnowledgePageRepository knowledgePageRepository,
            QuestionThreadRepository questionThreadRepository,
            SlugGenerator slugGenerator,
            KnowledgeContextService knowledgeContextService
    ) {
        this.aiAnswerGenerator = aiAnswerGenerator;
        this.knowledgePageRepository = knowledgePageRepository;
        this.questionThreadRepository = questionThreadRepository;
        this.slugGenerator = slugGenerator;
        this.knowledgeContextService = knowledgeContextService;
    }

    public QuestionSubmissionResult submit(QuestionSubmissionCommand command) {
        var existingPage = knowledgeContextService.findBestMatch(command.question());
        if (existingPage.isPresent()) {
            KnowledgePage page = existingPage.get();
            page.recordLinkedQuestion();
            KnowledgePage savedPage = knowledgePageRepository.save(page);
            QuestionThread savedThread = saveQuestionThread(
                    command,
                    knowledgeContextService.buildAnswerFromPage(savedPage),
                    savedPage.getId()
            );
            return new QuestionSubmissionResult(savedPage, savedThread, true);
        }

        AiAnswerDraft draft = aiAnswerGenerator.generate(command.question(), command.sourceType());
        KnowledgePage savedPage = saveKnowledgePage(draft, command);
        QuestionThread savedThread = saveQuestionThread(command, draft.answer(), savedPage.getId());
        return new QuestionSubmissionResult(savedPage, savedThread, false);
    }

    public QuestionThread submitQuestion(String author, String channel, String question, KnowledgeSourceType sourceType) {
        return submit(new QuestionSubmissionCommand(author, channel, question, sourceType)).thread();
    }

    private KnowledgePage saveKnowledgePage(AiAnswerDraft draft, QuestionSubmissionCommand command) {
        String slug = slugGenerator.generate(draft.title());
        KnowledgePage page = knowledgePageRepository.findBySlug(slug)
                .orElseGet(() -> KnowledgePage.create(
                        draft.title(),
                        slug,
                        draft.summary(),
                        "",
                        draft.tags(),
                        command.sourceType(),
                        KnowledgeStatus.DRAFT
                ));

        page.mergeAiDraft(draft, command.question());
        return knowledgePageRepository.save(page);
    }

    private QuestionThread saveQuestionThread(QuestionSubmissionCommand command, String answer, String linkedPageId) {
        QuestionThread thread = QuestionThread.create(
                command.author(),
                command.channel(),
                command.question(),
                answer,
                linkedPageId,
                command.sourceType()
        );
        return questionThreadRepository.save(thread);
    }
}