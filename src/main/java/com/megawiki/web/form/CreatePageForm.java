package com.megawiki.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreatePageForm {

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 300)
    private String summary;

    @NotBlank
    @Size(max = 4000)
    private String content;

    @Size(max = 300)
    private String tags = "onboarding, wiki";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}