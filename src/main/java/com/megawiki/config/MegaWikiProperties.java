package com.megawiki.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mega-wiki")
public class MegaWikiProperties {

    private String title = "Mega-Wiki";
    private String slogan = "AI plants the first answer and teammates refine it into lasting knowledge.";
    private String missionTrack = "Transform";
    private String aiProvider = "Gemini 2.5 Flash (adapter-ready)";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlogan() {
        return slogan;
    }

    public void setSlogan(String slogan) {
        this.slogan = slogan;
    }

    public String getMissionTrack() {
        return missionTrack;
    }

    public void setMissionTrack(String missionTrack) {
        this.missionTrack = missionTrack;
    }

    public String getAiProvider() {
        return aiProvider;
    }

    public void setAiProvider(String aiProvider) {
        this.aiProvider = aiProvider;
    }
}