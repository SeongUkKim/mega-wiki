package com.megawiki.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContributionForm {

    @NotBlank
    @Size(max = 30)
    private String author = "teammate";

    @NotBlank
    @Size(max = 2000)
    private String content;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}