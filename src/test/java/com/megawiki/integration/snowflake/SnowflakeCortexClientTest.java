package com.megawiki.integration.snowflake;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.megawiki.config.SnowflakeProperties;
import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SnowflakeCortexClientTest {

    private MockWebServer mockWebServer;
    private SnowflakeCortexClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        SnowflakeProperties properties = new SnowflakeProperties();
        properties.setEnabled(true);
        properties.setApiUrl(mockWebServer.url("/").toString());
        properties.setScoreThreshold(-7.0);
        properties.setTimeoutSeconds(10);

        client = new SnowflakeCortexClient(RestClient.builder(), properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void sendsQuestionAndParsesSuccessResponse() throws Exception {
        String responseBody = """
                {
                  "title": "8. 명함 신청하기",
                  "answer": "그룹웨어 기안 양식 중 명함신청서 양식을 통해 기안해 주세요.",
                  "reranker_score": -3.212328
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setBody(responseBody)
                .addHeader("Content-Type", "application/json"));

        SnowflakeCortexResponse response = client.search("명함 신청하는 방법이 궁금해");

        assertThat(response.title()).isEqualTo("8. 명함 신청하기");
        assertThat(response.answer()).contains("명함신청서");
        assertThat(response.rerankerScore()).isCloseTo(-3.212328, org.assertj.core.data.Offset.offset(0.001));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        String body = request.getBody().readUtf8();
        assertThat(body).contains("명함 신청하는 방법이 궁금해");
    }

    @Test
    void throwsOnServerError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("{\"error\": \"서버 오류\"}"));

        assertThatThrownBy(() -> client.search("test question"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void isRelevantReturnsTrueAboveThreshold() {
        SnowflakeCortexResponse response = new SnowflakeCortexResponse("title", "answer", -3.0);
        assertThat(client.isRelevant(response)).isTrue();
    }

    @Test
    void isRelevantReturnsFalseBelowThreshold() {
        SnowflakeCortexResponse response = new SnowflakeCortexResponse("title", "answer", -8.5);
        assertThat(client.isRelevant(response)).isFalse();
    }
}
