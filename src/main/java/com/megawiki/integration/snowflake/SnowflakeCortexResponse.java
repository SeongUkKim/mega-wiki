package com.megawiki.integration.snowflake;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SnowflakeCortexResponse(
        String title,
        String answer,
        @JsonProperty("reranker_score") double rerankerScore
) {
}
