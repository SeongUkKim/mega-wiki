package com.megawiki.service;

import com.megawiki.domain.ContributionType;
import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.KnowledgeStatus;
import com.megawiki.repository.KnowledgePageRepository;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class KnowledgePageService {

    private final KnowledgePageRepository knowledgePageRepository;
    private final SlugGenerator slugGenerator;

    public KnowledgePageService(KnowledgePageRepository knowledgePageRepository, SlugGenerator slugGenerator) {
        this.knowledgePageRepository = knowledgePageRepository;
        this.slugGenerator = slugGenerator;
    }

    public List<KnowledgePage> getRecentPages(int limit) {
        return knowledgePageRepository.findRecent(limit);
    }

    public List<KnowledgePage> getAllPages() {
        return knowledgePageRepository.findAll();
    }

    public KnowledgePage getPage(String pageId) {
        return knowledgePageRepository.findById(pageId)
                .orElseThrow(() -> new NoSuchElementException("Page not found: " + pageId));
    }

    public KnowledgePage createManualPage(String title, String summary, String content, String tagsText) {
        String uniqueSlug = createUniqueSlug(title);
        KnowledgePage page = KnowledgePage.create(
                title,
                uniqueSlug,
                summary,
                content,
                parseTags(tagsText),
                KnowledgeSourceType.MANUAL,
                KnowledgeStatus.CURATED
        );
        page.addContribution("Document Owner", ContributionType.MANUAL_EDIT, "Created a new wiki page manually.");
        return knowledgePageRepository.save(page);
    }

    public KnowledgePage addContribution(String pageId, String author, String content) {
        KnowledgePage page = getPage(pageId);
        page.addContribution(author, ContributionType.COLLEAGUE_FEEDBACK, content);
        return knowledgePageRepository.save(page);
    }

    public KnowledgePage markHelpful(String pageId) {
        KnowledgePage page = getPage(pageId);
        page.markHelpful();
        return knowledgePageRepository.save(page);
    }

    private String createUniqueSlug(String title) {
        String baseSlug = slugGenerator.generate(title);
        String candidate = baseSlug;
        int sequence = 2;
        while (knowledgePageRepository.findBySlug(candidate).isPresent()) {
            candidate = baseSlug + "-" + sequence;
            sequence += 1;
        }
        return candidate;
    }

    private Set<String> parseTags(String rawTags) {
        if (rawTags == null || rawTags.isBlank()) {
            return Set.of("onboarding");
        }
        return Arrays.stream(rawTags.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }
}