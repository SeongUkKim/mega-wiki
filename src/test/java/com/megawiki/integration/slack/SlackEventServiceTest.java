package com.megawiki.integration.slack;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megawiki.domain.KnowledgePage;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.domain.KnowledgeStatus;
import com.megawiki.domain.QuestionThread;
import com.megawiki.service.QuestionSubmissionCommand;
import com.megawiki.service.QuestionSubmissionResult;
import com.megawiki.service.QuestionWorkflowService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;

@ExtendWith(MockitoExtension.class)
class SlackEventServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TaskExecutor taskExecutor = Runnable::run;

    @Mock
    private QuestionWorkflowService questionWorkflowService;

    @Mock
    private SlackApiClient slackApiClient;

    private SlackEventService slackEventService;

    @BeforeEach
    void setUp() {
        SlackMentionProcessor mentionProcessor = new GeminiSlackMentionProcessor(questionWorkflowService, slackApiClient);
        slackEventService = new SlackEventService(
                slackApiClient,
                taskExecutor,
                new SlackEventDeduplicator(),
                List.of(mentionProcessor)
        );
    }

    @Test
    void processesAppMentionAndRepliesInThread() throws Exception {
        String payload = """
                {
                  "type": "event_callback",
                  "event_id": "Ev01",
                  "event": {
                    "type": "app_mention",
                    "user": "U123",
                    "text": "<@UBOT> Where is the remote work guide?",
                    "channel": "C123",
                    "ts": "1710000000.000100"
                  }
                }
                """;

        QuestionThread thread = QuestionThread.restore(
                "thread-1",
                "U123",
                "C123",
                "Where is the remote work guide?",
                "Use the HR portal and ask your lead to approve it.",
                "page-1",
                KnowledgeSourceType.SLACK_THREAD,
                LocalDateTime.now()
        );
        KnowledgePage page = KnowledgePage.create(
                "Remote Work Guide",
                "remote-work-guide",
                "Guide",
                "Content",
                Set.of("remote-work"),
                KnowledgeSourceType.SLACK_THREAD,
                KnowledgeStatus.DRAFT
        );
        page.assignId("page-1");

        when(questionWorkflowService.submit(new QuestionSubmissionCommand(
                "U123",
                "C123",
                "Where is the remote work guide?",
                KnowledgeSourceType.SLACK_THREAD
        ))).thenReturn(new QuestionSubmissionResult(page, thread));

        slackEventService.acceptEvent(objectMapper.readTree(payload));

        verify(slackApiClient).postThreadReply(
                eq("C123"),
                eq("1710000000.000100"),
                contains("*Remote Work Guide*")
        );
    }

    @Test
    void skipsDuplicateSlackEventIds() throws Exception {
        String payload = """
                {
                  "type": "event_callback",
                  "event_id": "Ev02",
                  "event": {
                    "type": "app_mention",
                    "user": "U123",
                    "text": "<@UBOT> help",
                    "channel": "C123",
                    "ts": "1710000000.000100"
                  }
                }
                """;

        QuestionThread thread = QuestionThread.restore(
                "thread-1",
                "U123",
                "C123",
                "help",
                "Answer",
                "page-1",
                KnowledgeSourceType.SLACK_THREAD,
                LocalDateTime.now()
        );
        KnowledgePage page = KnowledgePage.create(
                "General Onboarding Guide",
                "general-onboarding-guide",
                "Guide",
                "Content",
                Set.of("onboarding"),
                KnowledgeSourceType.SLACK_THREAD,
                KnowledgeStatus.DRAFT
        );
        page.assignId("page-1");

        when(questionWorkflowService.submit(new QuestionSubmissionCommand(
                "U123",
                "C123",
                "help",
                KnowledgeSourceType.SLACK_THREAD
        ))).thenReturn(new QuestionSubmissionResult(page, thread));

        slackEventService.acceptEvent(objectMapper.readTree(payload));
        slackEventService.acceptEvent(objectMapper.readTree(payload));

        verify(questionWorkflowService).submit(new QuestionSubmissionCommand(
                "U123",
                "C123",
                "help",
                KnowledgeSourceType.SLACK_THREAD
        ));
    }

    @Test
    void asksForQuestionWhenMentionContainsNoText() throws Exception {
        String payload = """
                {
                  "type": "event_callback",
                  "event_id": "Ev03",
                  "event": {
                    "type": "app_mention",
                    "user": "U123",
                    "text": "<@UBOT>",
                    "channel": "C123",
                    "ts": "1710000000.000100"
                  }
                }
                """;

        slackEventService.acceptEvent(objectMapper.readTree(payload));

        verifyNoInteractions(questionWorkflowService);
        verify(slackApiClient).postThreadReply(eq("C123"), eq("1710000000.000100"), contains("질문을 입력해 주세요"));
    }
}