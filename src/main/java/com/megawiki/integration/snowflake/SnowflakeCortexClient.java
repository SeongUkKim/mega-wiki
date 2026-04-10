package com.megawiki.integration.snowflake;

import com.megawiki.config.SnowflakeProperties;
import java.util.Map;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "mega-wiki.snowflake.enabled", havingValue = "true")
public class SnowflakeCortexClient {

    private final RestClient restClient;
    private final SnowflakeProperties properties;

    public SnowflakeCortexClient(RestClient.Builder restClientBuilder, SnowflakeProperties properties) {
        this.properties = properties;

        int timeoutMillis = Math.max(properties.getTimeoutSeconds(), 1) * 1000;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);

        this.restClient = restClientBuilder
                .baseUrl(properties.getApiUrl())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public SnowflakeCortexResponse search(String question) {
        return restClient.post()
                .body(Map.of("question", question))
                .retrieve()
                .body(SnowflakeCortexResponse.class);
    }

    public boolean isRelevant(SnowflakeCortexResponse response) {
        return response.rerankerScore() >= properties.getScoreThreshold();
    }
}