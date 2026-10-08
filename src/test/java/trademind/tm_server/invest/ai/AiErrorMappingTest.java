package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.InvestExceptionHandler;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** How AI failures reach the client: the right status, a safe message, and the Retry-After header. */
@WebMvcTest
@ContextConfiguration(classes = AiErrorMappingTest.TestApp.class)
@Import({AiController.class, GlobalExceptionHandler.class, InvestExceptionHandler.class})
class AiErrorMappingTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean UserContextService contextService;
    @MockitoBean AiService ai;
    @MockitoBean AiChatService chat;
    @MockitoBean AiProperties properties;
    @MockitoBean java.time.Clock clock;

    @Test
    void unavailableIs503WithTheReason() throws Exception {
        when(ai.usage()).thenThrow(new AiUnavailableException("AI is switched off. Set ai.enabled=true on the server"));
        mvc.perform(get("/api/v1/invest/ai/usage"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("AI is switched off. Set ai.enabled=true on the server"));
    }

    @Test
    void aLimitIs429WithRetryAfter() throws Exception {
        when(ai.usage()).thenThrow(new AiLimitExceededException("The daily AI limit (60 calls) is used up.", 4321));
        mvc.perform(get("/api/v1/invest/ai/usage"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "4321"))
                .andExpect(jsonPath("$.message").value("The daily AI limit (60 calls) is used up."));
    }

    @Test
    void anUpstreamFailureIs502WithAGenericMessage() throws Exception {
        when(ai.usage()).thenThrow(new AiUpstreamException());
        mvc.perform(get("/api/v1/invest/ai/usage"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("The AI service did not answer. Try again in a moment."));
    }

    @Test
    void anUnusableReplyIs502WithAGenericMessage() throws Exception {
        when(ai.usage()).thenThrow(new AiInvalidReplyException());
        mvc.perform(get("/api/v1/invest/ai/usage"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("The AI answered in a form that could not be used. Try again."));
    }
}
