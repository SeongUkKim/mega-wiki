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

    public QuestionWorkflowService(
            AiAnswerGenerator aiAnswerGenerator,
            KnowledgePageRepository knowledgePageRepository,
            QuestionThreadRepository questionThreadRepository,
            SlugGenerator slugGenerator
    ) {
        this.aiAnswerGenerator = aiAnswerGenerator;
        this.knowledgePageRepository = knowledgePageRepository;
        this.questionThreadRepository = questionThreadRepository;
        this.slugGenerator = slugGenerator;
    }

    public QuestionThread submitQuestion(String author, String channel, String question, KnowledgeSourceType sourceType) {
        AiAnswerDraft draft = aiAnswerGenerator.generate(question, sourceType);
        String slug = slugGenerator.generate(draft.title());

        KnowledgePage page = knowledgePageRepository.findBySlug(slug)
                .orElseGet(() -> KnowledgePage.create(
                        draft.title(),
                        slug,
                        draft.summary(),
                        "",
                        draft.tags(),
                        sourceType,
                        KnowledgeStatus.DRAFT
                ));

        page.mergeAiDraft(draft, question);
        KnowledgePage savedPage = knowledgePageRepository.save(page);

        QuestionThread thread = QuestionThread.create(
                author,
                channel,
                question,
                draft.answer(),
                savedPage.getId(),
                sourceType
        );
        return questionThreadRepository.save(thread);
    }
}