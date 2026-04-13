package com.megawiki.integration.slack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.megawiki.config.SlackProperties;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.model.event.MessageEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SlackSocketModeRunnerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SlackProperties slackProperties;

    @Mock
    private SlackEventService slackEventService;

    @Mock
    private EventsApiPayload<AppMentionEvent> payload;

    @Mock
    private EventsApiPayload<MessageEvent> messagePayload;

    private SlackSocketModeRunner runner;

    @BeforeEach
    void setUp() {
        runner = new SlackSocketModeRunner(slackProperties, objectMapper, slackEventService);
    }

    @Test
    void forwardsSocketModePayloadToSlackEventService() {
        AppMentionEvent event = new AppMentionEvent();
        event.setUser("U123");
        event.setText("<@UBOT> help");
        event.setChannel("C123");
        event.setTs("1710000000.000100");
        event.setThreadTs("1710000000.000050");

        when(payload.getEvent()).thenReturn(event);
        when(payload.getEventId()).thenReturn("EvSocket01");

        runner.forwardMentionEvent(payload);

        ArgumentCaptor<JsonNode> requestCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(slackEventService).acceptEvent(requestCaptor.capture());

        JsonNode request = requestCaptor.getValue();
        assertThat(request.path("type").asText()).isEqualTo("event_callback");
        assertThat(request.path("event_id").asText()).isEqualTo("EvSocket01");
        assertThat(request.path("event").path("type").asText()).isEqualTo("app_mention");
        assertThat(request.path("event").path("user").asText()).isEqualTo("U123");
        assertThat(request.path("event").path("thread_ts").asText()).isEqualTo("1710000000.000050");
    }

    @Test
    void forwardsSocketModeMessagePayloadToSlackEventService() {
        MessageEvent event = new MessageEvent();
        event.setUser("U123");
        event.setText("hello there");
        event.setChannel("D123");
        event.setChannelType("im");
        event.setTs("1710000000.000200");

        when(messagePayload.getEvent()).thenReturn(event);
        when(messagePayload.getEventId()).thenReturn("EvSocket02");

        runner.forwardMessageEvent(messagePayload);

        ArgumentCaptor<JsonNode> requestCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(slackEventService).acceptEvent(requestCaptor.capture());

        JsonNode request = requestCaptor.getValue();
        assertThat(request.path("event_id").asText()).isEqualTo("EvSocket02");
        assertThat(request.path("event").path("type").asText()).isEqualTo("message");
        assertThat(request.path("event").path("channel_type").asText()).isEqualTo("im");
        assertThat(request.path("event").path("channel").asText()).isEqualTo("D123");
    }

    @Test
    void fallsBackToTimestampWhenSocketModeEventIdIsMissing() {
        AppMentionEvent event = new AppMentionEvent();
        event.setUser("U123");
        event.setText("<@UBOT> help");
        event.setChannel("C123");
        event.setTs("1710000000.000100");

        when(payload.getEvent()).thenReturn(event);
        when(payload.getEventId()).thenReturn(null);

        runner.forwardMentionEvent(payload);

        ArgumentCaptor<JsonNode> requestCaptor = ArgumentCaptor.forClass(JsonNode.class);
        verify(slackEventService).acceptEvent(requestCaptor.capture());
        assertThat(requestCaptor.getValue().path("event_id").asText()).isEqualTo("1710000000.000100");
    }
}