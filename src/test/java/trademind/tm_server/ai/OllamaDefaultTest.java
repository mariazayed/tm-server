package trademind.tm_server.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import trademind.tm_server.invest.ai.AiProperties;
import trademind.tm_server.invest.ai.AiService;
import trademind.tm_server.invest.ai.AiUsageStore;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

/** application.properties alone (no profile, no database) makes AI ready with Ollama with the compatible client and no key. */
class OllamaDefaultTest {

    @Configuration
    @EnableConfigurationProperties(AiProperties.class)
    @Import({AiService.class, OpenAiCompatibleLlmClient.class, AnthropicLlmClient.class})
    static class Config {
        @Bean AiUsageStore store() { return mock(AiUsageStore.class); }
        @Bean Clock clock() { return Clock.systemUTC(); }
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(Config.class);

    @Test
    void ollamaIsTheDefault() {
        runner.run(ctx -> {
            AiService ai = ctx.getBean(AiService.class);
            assertEquals(true, ai.status().configured(), ai.status().reason());
            assertEquals("qwen2.5:7b-instruct", ai.status().model());
            assertInstanceOf(OpenAiCompatibleLlmClient.class, ctx.getBean(LlmClient.class));
        });
    }
}
