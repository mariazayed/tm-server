package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.InvestExceptionHandler;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = TagSuggestionsControllerTest.TestApp.class)
@Import({TagSuggestionsController.class, GlobalExceptionHandler.class, InvestExceptionHandler.class})
class TagSuggestionsControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean TagSuggestionsService service;

    private org.springframework.test.web.servlet.ResultActions call(String json) throws Exception {
        return mvc.perform(post("/api/v1/invest/ai/tag-suggestions").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void passesTheDraftThroughAndReturnsTheSuggestions() throws Exception {
        when(service.suggest("I was scared", "DECISION", 2, List.of("calm")))
                .thenReturn(new TagSuggestionsService.Response(List.of(new TagSuggestionsService.Suggestion("TAG", "fear", false, "scared"))));
        call("{\"body\":\"I was scared\",\"mood\":2,\"entryType\":\"DECISION\",\"existingTags\":[\"calm\"]}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0].kind").value("TAG"))
                .andExpect(jsonPath("$.suggestions[0].tag").value("fear"))
                .andExpect(jsonPath("$.suggestions[0].isNew").value(false))
                .andExpect(jsonPath("$.suggestions[0].reason").value("scared"));
        verify(service).suggest("I was scared", "DECISION", 2, List.of("calm"));
    }

    @Test
    void onlyTheBodyIsRequired() throws Exception {
        when(service.suggest("x", null, null, null)).thenReturn(new TagSuggestionsService.Response(List.of()));
        call("{\"body\":\"x\"}").andExpect(status().isOk()).andExpect(jsonPath("$.suggestions").isEmpty());
    }

    @Test
    void badInputIs400AndNothingIsCalled() throws Exception {
        call("{\"body\":\"  \"}").andExpect(status().isBadRequest());
        call("{}").andExpect(status().isBadRequest());
        call("{\"body\":\"" + "a".repeat(4001) + "\"}").andExpect(status().isBadRequest());
        call("{\"body\":\"x\",\"mood\":6}").andExpect(status().isBadRequest());
        call("{\"body\":\"x\",\"entryType\":\"NOPE\"}").andExpect(status().isBadRequest());
        call("{\"body\":\"x\",\"existingTags\":[\"a\",\"b\",\"c\",\"d\",\"e\",\"f\",\"g\",\"h\",\"i\",\"j\",\"k\"]}").andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void anUnavailableAiIs503() throws Exception {
        when(service.suggest(any(), any(), any(), any())).thenThrow(new AiUnavailableException("AI is switched off"));
        call("{\"body\":\"x\"}").andExpect(status().isServiceUnavailable());
    }
}
