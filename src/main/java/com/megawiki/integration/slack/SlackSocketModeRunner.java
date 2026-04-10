package com.megawiki.integration.slack;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.megawiki.config.SlackProperties;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(name = "mega-wiki.slack.mode", havingValue = "socket")
public class SlackSocketModeRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SlackSocketModeRunner.class);

    private final SlackProperties slackProperties;
    private final ObjectMapper objectMapper;
    private final SlackEventService slackEventService;

    public SlackSocketModeRunner(
            SlackProperties slackProperties,
            ObjectMapper objectMapper,
            SlackEventService slackEventService
    ) {
        this.slackProperties = slackProperties;
        this.objectMapper = objectMapper;
        this.slackEventService = slackEventService;
    }

    @Override
    public void run(String... args) throws Exception {
        if (!slackProperties.isEnabled()) {
            log.info("Slack integration is disabled, skipping Socket Mode startup");
            return;
        }

        AppConfig appConfig = AppConfig.builder()
                .singleTeamBotToken(slackProperties.getBotToken())
                .build();
        App app = new App(appConfig);

        app.event(AppMentionEvent.class, (payload, ctx) -> {
            forwardMentionEvent(payload);
            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(slackProperties.getAppToken(), app);
        socketModeApp.startAsync();
        log.info("Slack Socket Mode connected successfully");
    }

    void forwardMentionEvent(EventsApiPayload<AppMentionEvent> payload) {
        AppMentionEvent event = payload.getEvent();
        ObjectNode payloadNode = objectMapper.createObjectNode();
        payloadNode.put("type", "event_callback");
        payloadNode.put("event_id", resolveEventId(payload, event));

        ObjectNode eventNode = payloadNode.putObject("event");
        putIfHasText(eventNode, "type", event.getType());
        putIfHasText(eventNode, "bot_id", event.getBotId());
        putIfHasText(eventNode, "user", event.getUser());
        putIfHasText(eventNode, "text", event.getText());
        putIfHasText(eventNode, "channel", event.getChannel());
        putIfHasText(eventNode, "ts", event.getTs());
        putIfHasText(eventNode, "thread_ts", event.getThreadTs());

        slackEventService.acceptEvent(payloadNode);
    }

    private static String resolveEventId(EventsApiPayload<AppMentionEvent> payload, AppMentionEvent event) {
        if (StringUtils.hasText(payload.getEventId())) {
            return payload.getEventId();
        }

        return StringUtils.hasText(event.getTs()) ? event.getTs() : "";
    }

    private static void putIfHasText(ObjectNode node, String fieldName, String value) {
        if (StringUtils.hasText(value)) {
            node.put(fieldName, value);
        }
    }
}