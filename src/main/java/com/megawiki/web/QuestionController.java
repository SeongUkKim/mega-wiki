package com.megawiki.web;

import com.megawiki.domain.QuestionThread;
import com.megawiki.service.QuestionWorkflowService;
import com.megawiki.web.form.QuestionForm;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/questions")
public class QuestionController {

    private final QuestionWorkflowService questionWorkflowService;

    public QuestionController(QuestionWorkflowService questionWorkflowService) {
        this.questionWorkflowService = questionWorkflowService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuestionThread submitQuestion(@Valid @RequestBody QuestionForm questionForm) {
        return questionWorkflowService.submitQuestion(
                questionForm.getAuthor(),
                questionForm.getChannel(),
                questionForm.getQuestion(),
                questionForm.getSourceType()
        );
    }
}