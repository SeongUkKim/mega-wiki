package com.megawiki.service;

import com.megawiki.domain.KnowledgePage;
import com.megawiki.repository.KnowledgePageRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeContextService {

    private static final Map<String, String> TERM_ALIASES = Map.ofEntries(
            Map.entry("\uB450\uBC88", "\uC911\uBCF5"),
            Map.entry("\uC911\uBCF5", "\uC911\uBCF5"),
            Map.entry("\uC774\uC720", "\uC6D0\uC778"),
            Map.entry("\uC6D0\uC778", "\uC6D0\uC778"),
            Map.entry("\uC65C", "\uC6D0\uC778")
    );
    private static final Set<String> STOPWORDS = Set.of(
            "\uD558\uB294",
            "\uB418\uB294",
            "\uC624\uB294",
            "\uC788\uB294"
    );
    private static final List<String> KOREAN_SUFFIXES = List.of(
            "\uC73C\uB85C",
            "\uC5D0\uC11C",
            "\uC5D0\uAC8C",
            "\uAE4C\uC9C0",
            "\uBD80\uD130",
            "\uCC98\uB7FC",
            "\uBCF4\uB2E4",
            "\uC758",
            "\uC740",
            "\uB294",
            "\uC774",
            "\uAC00",
            "\uC744",
            "\uB97C",
            "\uC5D0",
            "\uC640",
            "\uACFC",
            "\uB3C4",
            "\uB85C"
    );

    private final KnowledgePageRepository knowledgePageRepository;

    public KnowledgeContextService(KnowledgePageRepository knowledgePageRepository) {
        this.knowledgePageRepository = knowledgePageRepository;
    }

    public String buildContext(String question) {
        List<String> questionTerms = tokenize(question);
        List<KnowledgePage> pages = knowledgePageRepository.findRecent(20).stream()
                .sorted(Comparator
                        .comparingInt((KnowledgePage page) -> score(page, question, questionTerms))
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

    public Optional<KnowledgePage> findBestMatch(String question) {
        List<String> questionTerms = tokenize(question);
        if (questionTerms.isEmpty()) {
            return Optional.empty();
        }

        int threshold = Math.max(3, Math.min(questionTerms.size() * 2, 8));
        return knowledgePageRepository.findRecent(50).stream()
                .map(page -> new ScoredPage(page, score(page, question, questionTerms)))
                .filter(candidate -> candidate.score() >= threshold)
                .sorted(Comparator.comparingInt(ScoredPage::score).reversed()
                        .thenComparing(candidate -> candidate.page().getUpdatedAt(), Comparator.reverseOrder()))
                .map(ScoredPage::page)
                .findFirst();
    }

    public String buildAnswerFromPage(KnowledgePage page) {
        if (page.getContent() != null && !page.getContent().isBlank()) {
            return page.getContent().trim();
        }
        if (page.getSummary() != null && !page.getSummary().isBlank()) {
            return page.getSummary().trim();
        }
        return page.getTitle();
    }

    private static int score(KnowledgePage page, String question, List<String> questionTerms) {
        String normalizedQuestion = normalizeForPhrase(question);
        Set<String> titleTerms = Set.copyOf(tokenize(page.getTitle()));
        Set<String> summaryTerms = Set.copyOf(tokenize(page.getSummary()));
        Set<String> contentTerms = Set.copyOf(tokenize(page.getContent()));

        String title = String.join(" ", titleTerms);
        String summary = String.join(" ", summaryTerms);
        String content = String.join(" ", contentTerms);

        int score = 0;
        if (!normalizedQuestion.isBlank()) {
            if (title.contains(normalizedQuestion)) {
                score += 10;
            } else if (summary.contains(normalizedQuestion)) {
                score += 6;
            } else if (content.contains(normalizedQuestion)) {
                score += 3;
            }
        }

        score += overlapScore(questionTerms, titleTerms, 4);
        score += overlapScore(questionTerms, summaryTerms, 2);
        score += overlapScore(questionTerms, contentTerms, 1);

        long titleMatches = questionTerms.stream().filter(titleTerms::contains).count();
        if (titleMatches >= 2) {
            score += (int) titleMatches * 2;
        }
        return score;
    }

    private static int overlapScore(List<String> questionTerms, Set<String> pageTerms, int weight) {
        return questionTerms.stream()
                .filter(pageTerms::contains)
                .mapToInt(term -> weight)
                .sum();
    }

    private static List<String> tokenize(String text) {
        return Arrays.stream(normalize(text).split("[^a-z0-9\\uac00-\\ud7a3]+"))
                .map(String::trim)
                .map(KnowledgeContextService::canonicalize)
                .filter(term -> term.length() >= 2)
                .filter(term -> !STOPWORDS.contains(term))
                .distinct()
                .toList();
    }

    private static String normalizeForPhrase(String text) {
        return String.join(" ", tokenize(text));
    }

    private static String canonicalize(String term) {
        if (term == null || term.isBlank()) {
            return "";
        }
        String stripped = stripKoreanSuffix(term);
        return TERM_ALIASES.getOrDefault(stripped, stripped);
    }

    private static String stripKoreanSuffix(String term) {
        for (String suffix : KOREAN_SUFFIXES) {
            if (term.endsWith(suffix)) {
                String candidate = term.substring(0, term.length() - suffix.length()).trim();
                if (candidate.length() >= 2) {
                    return candidate;
                }
            }
        }
        return term;
    }

    private static String normalize(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\uac00-\\ud7a3]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private static String excerpt(String content) {
        if (content == null || content.isBlank()) {
            return "(empty)";
        }
        String normalized = content.replace("\r", " ").replace("\n", " ").trim();
        return normalized.length() <= 280 ? normalized : normalized.substring(0, 277) + "...";
    }

    private record ScoredPage(KnowledgePage page, int score) {
    }
}