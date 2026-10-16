package trademind.tm_server.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/** {@code ai.provider.type} decides which client exists: Anthropic by default, the compatible one for openai/ollama. */
class ProviderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AnthropicLlmClient.class, OpenAiCompatibleLlmClient.class);

    @Test
    void anthropicIsTheDefault() {
        runner.run(ctx -> {
            assertEquals(1, ctx.getBeansOfType(LlmClient.class).size());
            assertInstanceOf(AnthropicLlmClient.class, ctx.getBean(LlmClient.class));
        });
    }

    @Test
    void openAiAndOllamaUseTheCompatibleClient() {
        for (String type : new String[] {"openai", "ollama"}) {
            runner.withPropertyValues("ai.provider.type=" + type).run(ctx -> {
                assertEquals(1, ctx.getBeansOfType(LlmClient.class).size(), type);
                assertInstanceOf(OpenAiCompatibleLlmClient.class, ctx.getBean(LlmClient.class), type);
            });
        }
    }
}
