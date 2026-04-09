package com.megawiki.service;

import com.megawiki.domain.KnowledgePage;
import com.megawiki.repository.KnowledgePageRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeContextService {

    private final KnowledgePageRepository knowledgePageRepository;

    public KnowledgeContextService(KnowledgePageRepository knowledgePageRepository) {
        this.knowledgePageRepository = knowledgePageRepository;
    }

    public String buildContext(String question) {
        List<String> questionTerms = tokenize(question);
        List<KnowledgePage> pages = knowledgePageRepository.findRecent(20).stream()
                .sorted(Comparator
                        .comparingInt((KnowledgePage page) -> score(page, questionTerms))
                        .reversed()
                        .thenComparing(KnowledgePage::getUpdatedAt, Comparator.reverseOrder()))
                .limit(5)
                .toList();

        if (pages.isEmpty()) {
            return "No prior Mega-Wiki pages are available yet.";
        }

        return pages.stream()
                .map(page -> "Title: " + page.getTitle()
                        + "\nSummary: " + page.getSummary()
                        + "\nTags: " + String.join(", ", page.getTags())
                        + "\nContent excerpt: " + excerpt(page.getContent()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private static int score(KnowledgePage page, List<String> questionTerms) {
        String document = (page.getTitle() + " " + page.getSummary() + " " + page.getContent())
                .toLowerCase(Locale.ROOT);
        return (int) questionTerms.stream().filter(document::contains).count();
    }

    private static List<String> tokenize(String text) {
        return List.of(text.toLowerCase(Locale.ROOT).split("[^a-z0-9\uac00-\ud7a3]+"))
                .stream()
                .map(String::trim)
                .filter(term -> term.length() >= 2)
                .distinct()
                .toList();
    }

    private static String excerpt(String content) {
        if (content == null || content.isBlank()) {
            return "(empty)";
        }
        String normalized = content.replace("\r", " ").replace("\n", " ").trim();
        return normalized.length() <= 280 ? normalized : normalized.substring(0, 277) + "...";
    }
}
