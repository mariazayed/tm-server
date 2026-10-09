package trademind.tm_server.invest.rulecheck;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.InvestExceptionHandler;
import trademind.tm_server.invest.ai.RuleCheckAiService;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = RuleCheckControllerTest.TestApp.class)
@Import({RuleCheckController.class, GlobalExceptionHandler.class, InvestExceptionHandler.class})
class RuleCheckControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean RuleCheckService service;
    @MockitoBean RuleCheckAiService aiService;

    private org.springframework.test.web.servlet.ResultActions call(String json) throws Exception {
        return mvc.perform(post("/api/v1/invest/ai/rule-check").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void returnsTheFindingsAndTheChecksThatRan() throws Exception {
        LocalDate from = LocalDate.parse("2026-09-15");
        LocalDate to = LocalDate.parse("2026-10-15");
        when(service.run(from, to)).thenReturn(new RuleCheckService.Result(from, to, 72,
                List.of(RuleFinding.deterministic("WAIT_PERIOD", LocalDate.parse("2026-10-02"), "Entry 2 ...", List.of(1L, 2L))),
                List.of(new RuleCheckService.CheckInfo("WAIT_PERIOD", "Wait before a deviation", "RAN", null),
                        new RuleCheckService.CheckInfo("PLAN_CHANGE", "Plan changed without a note", "OFF", "why"))));
        call("{\"from\":\"2026-09-15\",\"to\":\"2026-10-15\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.from").value("2026-09-15"))
                .andExpect(jsonPath("$.waitHours").value(72))
                .andExpect(jsonPath("$.findings.length()").value(1))
                .andExpect(jsonPath("$.findings[0].check").value("WAIT_PERIOD"))
                .andExpect(jsonPath("$.findings[0].source").value("DETERMINISTIC"))
                .andExpect(jsonPath("$.findings[0].date").value("2026-10-02"))
                .andExpect(jsonPath("$.findings[0].entryIds[1]").value(2))
                .andExpect(jsonPath("$.findings[0].confidence").value("HIGH"))
                .andExpect(jsonPath("$.findings[0].ruleId").doesNotExist())
                .andExpect(jsonPath("$.checks[1].status").value("OFF"))
                .andExpect(jsonPath("$.checks[1].note").value("why"))
                .andExpect(jsonPath("$.model.requested").value(false))
                .andExpect(jsonPath("$.model.ran").value(false));
        verifyNoInteractions(aiService); // the model is only used when asked for
    }

    @Test
    void theModelPartRunsOnlyWhenAskedFor() throws Exception {
        LocalDate from = LocalDate.parse("2026-09-15");
        LocalDate to = LocalDate.parse("2026-10-15");
        RuleFinding model = RuleFinding.fromModel(LocalDate.parse("2026-10-05"), "Sold in a crash.", List.of(12L), 5L,
                "sold everything", "HIGH");
        when(aiService.run(from, to)).thenReturn(new RuleCheckResponse(from, to, 72, List.of(model), List.of(),
                new RuleCheckResponse.ModelInfo(true, true, null, 3, 1, 2)));
        call("{\"from\":\"2026-09-15\",\"to\":\"2026-10-15\",\"includeModel\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.findings[0].source").value("MODEL"))
                .andExpect(jsonPath("$.findings[0].ruleId").value(5))
                .andExpect(jsonPath("$.findings[0].quote").value("sold everything"))
                .andExpect(jsonPath("$.findings[0].confidence").value("HIGH"))
                .andExpect(jsonPath("$.model.requested").value(true))
                .andExpect(jsonPath("$.model.ran").value(true))
                .andExpect(jsonPath("$.model.entriesChecked").value(3))
                .andExpect(jsonPath("$.model.entriesSkipped").value(1))
                .andExpect(jsonPath("$.model.dropped").value(2));
        verifyNoInteractions(service);

        when(service.run(from, to)).thenReturn(new RuleCheckService.Result(from, to, 72, List.of(), List.of()));
        call("{\"from\":\"2026-09-15\",\"to\":\"2026-10-15\",\"includeModel\":false}").andExpect(status().isOk());
        org.mockito.Mockito.verify(service).run(from, to);
    }

    @Test
    void missingOrMalformedDatesAreA400AndNothingRuns() throws Exception {
        for (String body : new String[] {"{}", "{\"from\":\"2026-09-15\"}", "{\"from\":\"soon\",\"to\":\"2026-10-15\"}", "nope"}) {
            call(body).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void aBadRangeFromTheServiceIsA400WithItsMessage() throws Exception {
        when(service.run(LocalDate.parse("2026-01-01"), LocalDate.parse("2026-10-15")))
                .thenThrow(new BadRequestException("Choose a range of at most 92 days"));
        call("{\"from\":\"2026-01-01\",\"to\":\"2026-10-15\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choose a range of at most 92 days"));
    }
}
