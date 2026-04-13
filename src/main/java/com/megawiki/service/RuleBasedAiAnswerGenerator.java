package com.megawiki.service;

import com.megawiki.domain.KnowledgeSourceType;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RuleBasedAiAnswerGenerator implements AiAnswerGenerator {

    private static final String KOR_REMOTE = "\uC7AC\uD0DD";
    private static final String KOR_HYBRID = "\uC6D0\uACA9";
    private static final String KOR_ASK = "\uC5D0\uC2A4\uD06C";
    private static final String KOR_INQUIRY = "\uBB38\uC758";
    private static final String KOR_SLACK = "\uC2AC\uB799";
    private static final String KOR_THREAD = "\uC2A4\uB808\uB4DC";

    private final List<Playbook> playbooks = List.of(
            new Playbook(
                    "Remote Work Guide",
                    Set.of(KOR_REMOTE, KOR_HYBRID, "remote"),
                    "Remote work questions should capture the approval path, latest UI changes, and practical team guidance.",
                    "Remote work requests usually start in the internal HR portal, then move through approval. If the UI changed this week, capture the latest screen and append it to the wiki page immediately.",
                    "## Request flow\n1. Open the internal HR portal\n2. Choose the remote work option\n3. Add date and reason\n4. Send the request to the team lead\n\n## Team tips\n- Morning requests tend to move faster.\n- If a screen changes, update the documentation with a fresh screenshot in the same day.",
                    Set.of("onboarding", "hr", "remote-work")
            ),
            new Playbook(
                    "MegaOne Ask Sharing Guide",
                    Set.of(KOR_ASK, "ask", KOR_INQUIRY),
                    "MegaOne Ask answers should be promoted into shared documentation when they are likely to repeat.",
                    "When an answer has FAQ value, remove sensitive details, summarize the answer, and store the summary plus source link in Mega-Wiki.",
                    "## Ask to wiki conversion\n1. Check recurring FAQ potential\n2. Remove private details\n3. Write a three-line summary\n4. Publish the summary with source metadata\n\n## Operating tips\n- The original source link improves trust.\n- Merge into an existing page when the topic already exists.",
                    Set.of("faq", "ask", "knowledge-share")
            ),
            new Playbook(
                    "Slack Knowledge Archiving Policy",
                    Set.of(KOR_SLACK, "thread", KOR_THREAD),
                    "Slack is fast but disposable, so useful threads should be structured and preserved quickly.",
                    "If you combine the original question, the AI draft, and teammate corrections into one page, the next employee can solve the same issue through search instead of asking again.",
                    "## Archiving criteria\n- The same question appears two or more times\n- A thread contains team-specific practical tips\n- A policy or UI changed and needs current-state documentation\n\n## Recommended document sections\n- Why the question happens\n- Initial AI answer\n- Teammate corrections\n- Latest update history",
                    Set.of("slack", "operations", "collective-intelligence")
            )
    );

    @Override
    public AiAnswerDraft generate(String question, KnowledgeSourceType sourceType) {
        String normalizedQuestion = question.toLowerCase(Locale.ROOT);
        return playbooks.stream()
                .filter(playbook -> playbook.matches(normalizedQuestion))
                .findFirst()
                .map(Playbook::toDraft)
                .orElseGet(() -> fallbackDraft(question, sourceType));
    }

    private AiAnswerDraft fallbackDraft(String question, KnowledgeSourceType sourceType) {
        String sourceLabel = switch (sourceType) {
            case SLACK_THREAD -> "Slack question";
            case MEGAONE_ASK -> "MegaOne Ask request";
            case MANUAL -> "manual document";
        };
        Set<String> tags = new LinkedHashSet<>(Set.of("onboarding", "knowledge-management"));
        return new AiAnswerDraft(
                fallbackTitle(question, sourceType),
                sourceLabel + " should be normalized into background, answer, and next action before it becomes a durable wiki page.",
                "Separate context, source references, and concrete action items first. Then decide whether the issue has enough repeat value to archive.",
                "## Drafting template\n- Question background: why this repeats\n- Immediate answer: what the employee should do now\n- Source reference: where the answer came from\n- Follow-up action: what should change in process or documentation\n\nOriginal question\n" + question,
                tags
        );
    }

    private String fallbackTitle(String question, KnowledgeSourceType sourceType) {
        String normalizedQuestion = question == null ? "" : question.trim().replaceAll("\\s+", " ");
        if (!normalizedQuestion.isBlank()) {
            return normalizedQuestion.length() <= 60
                    ? normalizedQuestion
                    : normalizedQuestion.substring(0, 57) + "...";
        }
        return switch (sourceType) {
            case SLACK_THREAD -> "Slack question draft";
            case MEGAONE_ASK -> "MegaOne Ask draft";
            case MANUAL -> "Manual draft";
        };
    }

    private record Playbook(
            String title,
            Set<String> keywords,
            String summary,
            String answer,
            String content,
            Set<String> tags
    ) {
        private boolean matches(String question) {
            return keywords.stream().map(keyword -> keyword.toLowerCase(Locale.ROOT)).anyMatch(question::contains);
        }

        private AiAnswerDraft toDraft() {
            return new AiAnswerDraft(title, summary, answer, content, tags);
        }
    }
}