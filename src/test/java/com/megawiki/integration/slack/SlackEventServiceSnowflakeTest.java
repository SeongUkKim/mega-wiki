package com.megawiki.integration.slack;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megawiki.config.SnowflakeProperties;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.integration.snowflake.SnowflakeCortexClient;
import com.megawiki.integration.snowflake.SnowflakeCortexResponse;
import com.megawiki.repository.KnowledgePageRepository;
import com.megawiki.service.QuestionWorkflowService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

@ExtendWith(MockitoExtension.class)
class SlackEventServiceSnowflakeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TaskExecutor taskExecutor = Runnable::run;

    @Mock
    private QuestionWorkflowService questionWorkflowService;

    @Mock
    private KnowledgePageRepository knowledgePageRepository;

    @Mock
    private SlackApiClient slackApiClient;

    @Mock
    private SnowflakeCortexClient snowflakeCortexClient;

    private SlackEventService slackEventService;

    @BeforeEach
    void setUp() {
        SnowflakeProperties snowflakeProperties = new SnowflakeProperties();
        snowflakeProperties.setEnabled(true);
        snowflakeProperties.setScoreThreshold(-7.0);

        slackEventService = new SlackEventService(
                questionWorkflowService,
                knowledgePageRepository,
                slackApiClient,
                taskExecutor,
                new SlackEventDeduplicator(),
                snowflakeCortexClient,
                snowflakeProperties
        );
    }

    @Test
    void routesToSnowflakeWhenEnabled() throws Exception {
        String payload = """
                {
                  "type": "event_callback",
                  "event_id": "EvSf01",
                  "event": {
                    "type": "app_mention",
                    "user": "U123",
                    "text": "<@UBOT> 명함 신청하는 방법이 궁금해",
                    "channel": "C123",
                    "ts": "1710000000.000100"
                  }
                }
                """;

        SnowflakeCortexResponse cortexResponse = new SnowflakeCortexResponse(
                "8. 명함 신청하기",
                "그룹웨어 기안 양식 중 명함신청서 양식을 통해 기안해 주세요.",
                -3.212328
        );
        when(snowflakeCortexClient.search("명함 신청하는 방법이 궁금해")).thenReturn(cortexResponse);
        when(snowflakeCortexClient.isRelevant(cortexResponse)).thenReturn(true);

        slackEventService.acceptEvent(objectMapper.readTree(payload));

        verify(slackApiClient).postThreadReply(
                eq("C123"),
                eq("1710000000.000100"),
                contains("명함 신청하기")
        );
        verify(questionWorkflowService, never()).submitQuestion(
                eq("U123"), eq("C123"), eq("명함 신청하는 방법이 궁금해"),
                eq(KnowledgeSourceType.SLACK_THREAD)
        );
    }

    @Test
    void repliesNotFoundWhenScoreBelowThreshold() throws Exception {
        String payload = """
                {
                  "type": "event_callback",
                  "event_id": "EvSf02",
                  "event": {
                    "type": "app_mention",
                    "user": "U123",
                    "text": "<@UBOT> 우주여행 가는 방법",
                    "channel": "C123",
                    "ts": "1710000000.000200"
                  }
                }
                """;

        SnowflakeCortexResponse cortexResponse = new SnowflakeCortexResponse(
                "관련 없는 문서",
                "답변 내용",
                -8.5
        );
        when(snowflakeCortexClient.search("우주여행 가는 방법")).thenReturn(cortexResponse);
        when(snowflakeCortexClient.isRelevant(cortexResponse)).thenReturn(false);

        slackEventService.acceptEvent(objectMapper.readTree(payload));

        verify(slackApiClient).postThreadReply(
                eq("C123"),
                eq("1710000000.000200"),
                contains("등록되어 있지 않은 질문")
        );
    }

    @Test
    void repliesErrorWhenSnowflakeThrows() throws Exception {
        String payload = """
                {
                  "type": "event_callback",
                  "event_id": "EvSf03",
                  "event": {
                    "type": "app_mention",
                    "user": "U123",
                    "text": "<@UBOT> 테스트 질문",
                    "channel": "C123",
                    "ts": "1710000000.000300"
                  }
                }
                """;

        when(snowflakeCortexClient.search("테스트 질문")).thenThrow(new RuntimeException("API timeout"));

        slackEventService.acceptEvent(objectMapper.readTree(payload));

        verify(slackApiClient).postThreadReply(
                eq("C123"),
                eq("1710000000.000300"),
                contains("오류가 발생했습니다")
        );
    }
}
