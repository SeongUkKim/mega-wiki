package com.megawiki.web.form;

import com.megawiki.domain.KnowledgeSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class QuestionForm {

    @NotBlank
    @Size(max = 30)
    private String author = "new-hire";

    @NotBlank
    @Size(max = 30)
    private String channel = "#general";

    @NotBlank
    @Size(max = 500)
    private String question;

    private KnowledgeSourceType sourceType = KnowledgeSourceType.SLACK_THREAD;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public KnowledgeSourceType getSourceType() {
        return sourceType;
    }

    public void setSourceType(KnowledgeSourceType sourceType) {
        this.sourceType = sourceType;
    }
}