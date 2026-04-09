package com.megawiki.integration.slack;

import com.megawiki.config.SlackProperties;
import com.megawiki.config.SnowflakeProperties;
import com.megawiki.integration.snowflake.SnowflakeCortexClient;
import com.megawiki.integration.snowflake.SnowflakeCortexResponse;
import com.slack.api.app_backend.events.payload.EventsApiPayload;
import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.socket_mode.SocketModeApp;
import com.slack.api.model.event.AppMentionEvent;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "mega-wiki.slack.mode", havingValue = "socket")
public class SlackSocketModeRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SlackSocketModeRunner.class);

    private final SlackProperties slackProperties;
    private final SnowflakeProperties snowflakeProperties;
    private final SnowflakeCortexClient snowflakeCortexClient;

    public SlackSocketModeRunner(
            SlackProperties slackProperties,
            SnowflakeProperties snowflakeProperties,
            @Nullable SnowflakeCortexClient snowflakeCortexClient
    ) {
        this.slackProperties = slackProperties;
        this.snowflakeProperties = snowflakeProperties;
        this.snowflakeCortexClient = snowflakeCortexClient;
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
            handleMention(payload, ctx.client());
            return ctx.ack();
        });

        SocketModeApp socketModeApp = new SocketModeApp(slackProperties.getAppToken(), app);
        socketModeApp.startAsync();
        log.info("Slack Socket Mode connected successfully");
    }

    private void handleMention(EventsApiPayload<AppMentionEvent> payload, MethodsClient client) {
        AppMentionEvent event = payload.getEvent();
        String channel = event.getChannel();
        String threadTs = event.getThreadTs() != null ? event.getThreadTs() : event.getTs();
        String question = normalizeQuestion(event.getText());

        if (question.isBlank()) {
            postReply(client, channel, threadTs, "질문을 입력해 주세요. 봇 멘션 뒤에 궁금한 내용을 적어 주세요.");
            return;
        }

        if (snowflakeProperties.isEnabled() && snowflakeCortexClient != null) {
            processWithSnowflake(client, channel, threadTs, question);
        } else {
            postReply(client, channel, threadTs, "Snowflake 연동이 비활성화되어 있습니다.");
        }
    }

    private void processWithSnowflake(MethodsClient client, String channel, String threadTs, String question) {
        try {
            SnowflakeCortexResponse response = snowflakeCortexClient.search(question);
            if (snowflakeCortexClient.isRelevant(response)) {
                String reply = "*" + response.title() + "*\n\n" + response.answer();
                postReply(client, channel, threadTs, reply);
            } else {
                postReply(client, channel, threadTs, "등록되어 있지 않은 질문입니다. 다른 키워드로 다시 질문해 주세요.");
            }
        } catch (Exception e) {
            log.error("Snowflake Cortex search failed", e);
            postReply(client, channel, threadTs, "답변 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private void postReply(MethodsClient client, String channel, String threadTs, String text) {
        try {
            client.chatPostMessage(ChatPostMessageRequest.builder()
                    .channel(channel)
                    .threadTs(threadTs)
                    .text(text)
                    .build());
        } catch (Exception e) {
            log.error("Failed to post Slack reply", e);
        }
    }

    private static String normalizeQuestion(String rawText) {
        return rawText.replaceAll("<@[^>]+>", " ")
                .replace('&', ' ')
                .trim()
                .replaceAll("\\s+", " ");
    }
}
