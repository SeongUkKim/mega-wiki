package com.megawiki.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megawiki.config.GeminiProperties;
import com.megawiki.domain.KnowledgeSourceType;
import com.megawiki.repository.InMemoryKnowledgePageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GeminiAiAnswerGeneratorTest {

    @Test
    void fallsBackToRuleBasedGeneratorWhenGeminiIsDisabled() {
        GeminiProperties geminiProperties = new GeminiProperties();
        geminiProperties.setEnabled(false);

        GeminiAiAnswerGenerator generator = new GeminiAiAnswerGenerator(
                RestClient.builder(),
                new ObjectMapper(),
                geminiProperties,
                new KnowledgeContextService(new InMemoryKnowledgePageRepository()),
                new RuleBasedAiAnswerGenerator()
        );

        AiAnswerDraft draft = generator.generate("Where is the remote work guide?", KnowledgeSourceType.SLACK_THREAD);

        assertThat(draft.title()).isEqualTo("Remote Work Guide");
    }

    @Test
    void parsesGeminiJsonResponseIntoDraft() {
        GeminiProperties geminiProperties = new GeminiProperties();
        geminiProperties.setEnabled(true);
        geminiProperties.setApiKey("test-key");
        geminiProperties.setModel("gemini-2.5-flash");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=test-key"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "candidates": [
                            {
                              "content": {
                                "parts": [
                                  {
                                    "text": "{\"title\":\"Remote Work Guide\",\"summary\":\"Use the HR portal and current team policy.\",\"answer\":\"Open the HR portal, submit the request, and confirm the latest UI with your lead.\",\"content\":\"## Steps\\n1. Open the HR portal\\n2. Submit the request\\n3. Confirm policy updates with the team lead\",\"tags\":[\"remote-work\",\"hr\",\"onboarding\"]}"
                                  }
                                ]
                              }
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        GeminiAiAnswerGenerator generator = new GeminiAiAnswerGenerator(
                builder,
                new ObjectMapper(),
                geminiProperties,
                new KnowledgeContextService(new InMemoryKnowledgePageRepository()),
                new RuleBasedAiAnswerGenerator()
        );

        AiAnswerDraft draft = generator.generate("Where is the remote work guide?", KnowledgeSourceType.SLACK_THREAD);

        assertThat(draft.title()).isEqualTo("Remote Work Guide");
        assertThat(draft.tags()).contains("remote-work", "hr", "onboarding");
        assertThat(draft.answer()).contains("HR portal");
        server.verify();
    }
}
