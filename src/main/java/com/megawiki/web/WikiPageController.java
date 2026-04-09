package com.megawiki.web;

import com.megawiki.domain.KnowledgePage;
import com.megawiki.service.KnowledgePageService;
import com.megawiki.web.form.ContributionForm;
import com.megawiki.web.form.CreatePageForm;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pages")
public class WikiPageController {

    private final KnowledgePageService knowledgePageService;

    public WikiPageController(KnowledgePageService knowledgePageService) {
        this.knowledgePageService = knowledgePageService;
    }

    @GetMapping
    public List<KnowledgePage> pages() {
        return knowledgePageService.getAllPages();
    }

    @GetMapping("/{pageId}")
    public KnowledgePage page(@PathVariable String pageId) {
        return knowledgePageService.getPage(pageId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public KnowledgePage createPage(@Valid @RequestBody CreatePageForm createPageForm) {
        return knowledgePageService.createManualPage(
                createPageForm.getTitle(),
                createPageForm.getSummary(),
                createPageForm.getContent(),
                createPageForm.getTags()
        );
    }

    @PostMapping("/{pageId}/contributions")
    public KnowledgePage addContribution(@PathVariable String pageId, @Valid @RequestBody ContributionForm contributionForm) {
        return knowledgePageService.addContribution(pageId, contributionForm.getAuthor(), contributionForm.getContent());
    }

    @PostMapping("/{pageId}/helpful")
    public KnowledgePage markHelpful(@PathVariable String pageId) {
        return knowledgePageService.markHelpful(pageId);
    }
}