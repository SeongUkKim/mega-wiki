package com.megawiki.config;

import com.megawiki.domain.ContributionType;
import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.KnowledgeStatus;
import com.megawiki.repository.KnowledgePageRepository;
import com.megawiki.service.SlugGenerator;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mega-wiki.storage", havingValue = "memory")
public class SeedDataInitializer implements ApplicationRunner {

    private final KnowledgePageRepository knowledgePageRepository;
    private final SlugGenerator slugGenerator;

    public SeedDataInitializer(KnowledgePageRepository knowledgePageRepository, SlugGenerator slugGenerator) {
        this.knowledgePageRepository = knowledgePageRepository;
        this.slugGenerator = slugGenerator;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (knowledgePageRepository.count() > 0) {
            return;
        }

        KnowledgePage remoteWorkGuide = KnowledgePage.create(
                "Remote Work Guide",
                slugGenerator.generate("Remote Work Guide"),
                "A starter guide that reduces repetitive questions about remote work requests.",
                "## Request flow\n1. Open the internal HR portal\n2. Choose the remote work option\n3. Enter date and reason\n4. Send approval request to the team lead\n\n## Field tips\n- Early morning requests usually move faster.\n- If the UI changes, capture the latest screen and update the page immediately.",
                tags("onboarding", "hr", "remote-work"),
                KnowledgeSourceType.SLACK_THREAD,
                KnowledgeStatus.CURATED
        );
        remoteWorkGuide.addContribution("Onboarding TF", ContributionType.MANUAL_EDIT,
                "Documented the baseline flow and practical field tips.");
        knowledgePageRepository.save(remoteWorkGuide);

        KnowledgePage askGuide = KnowledgePage.create(
                "MegaOne Ask Sharing Guide",
                slugGenerator.generate("MegaOne Ask Sharing Guide"),
                "A guide for converting one-off support answers into reusable team knowledge.",
                "## Recommended steps\n1. Remove sensitive details from the original answer\n2. Decide whether the topic has recurring FAQ value\n3. Publish a short summary plus the original source link in Mega-Wiki",
                tags("faq", "ask", "knowledge-share"),
                KnowledgeSourceType.MEGAONE_ASK,
                KnowledgeStatus.CURATED
        );
        askGuide.addContribution("Operations Team", ContributionType.MANUAL_EDIT,
                "Defined the flow for promoting repeated Ask answers into wiki assets.");
        knowledgePageRepository.save(askGuide);
    }

    private static Set<String> tags(String... values) {
        return new LinkedHashSet<>(Set.of(values));
    }
}