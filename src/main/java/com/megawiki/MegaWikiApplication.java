package com.megawiki;

import com.megawiki.config.GeminiProperties;
import com.megawiki.config.MegaWikiProperties;
import com.megawiki.config.NotionProperties;
import com.megawiki.config.SlackProperties;
import com.megawiki.config.SnowflakeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        MegaWikiProperties.class,
        NotionProperties.class,
        SlackProperties.class,
        GeminiProperties.class,
        SnowflakeProperties.class
})
public class MegaWikiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MegaWikiApplication.class, args);
    }
}