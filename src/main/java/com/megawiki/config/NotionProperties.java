package com.megawiki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mega-wiki.notion")
public class NotionProperties {

    private String apiToken = "";
    private String pagesDataSourceId = "";
    private String questionsDataSourceId = "";
    private String apiVersion = "2025-09-03";

    public String getApiToken() {
        return apiToken;
    }

    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getPagesDataSourceId() {
        return pagesDataSourceId;
    }

    public void setPagesDataSourceId(String pagesDataSourceId) {
        this.pagesDataSourceId = pagesDataSourceId;
    }

    public String getQuestionsDataSourceId() {
        return questionsDataSourceId;
    }

    public void setQuestionsDataSourceId(String questionsDataSourceId) {
        this.questionsDataSourceId = questionsDataSourceId;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }
}
