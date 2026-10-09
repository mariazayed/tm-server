package trademind.tm_server.invest.checkin;

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
import trademind.tm_server.invest.ai.CheckinSummaryAiService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = CheckinSummaryControllerTest.TestApp.class)
@Import({CheckinSummaryController.class, GlobalExceptionHandler.class, InvestExceptionHandler.class})
class CheckinSummaryControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean CheckinSummaryAiService service;

    private org.springframework.test.web.servlet.ResultActions call(String json) throws Exception {
        return mvc.perform(post("/api/v1/invest/ai/checkin-summary").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static CheckinSummaryResponse response(String text, CheckinSummaryResponse.ModelInfo model) {
        CheckinFacts facts = new CheckinFacts("2026-09", "USD", 30000, 1, LocalDate.parse("2026-09-01"), "DONE",
                List.of(new CheckinFacts.Buy(LocalDate.parse("2026-09-05"), 30000, new BigDecimal("3"), new BigDecimal("100.0000"), 0)),
                30000, 0, new BigDecimal("3"), new BigDecimal("100.0000"), LocalDate.parse("2026-09-05"), 4, 2, 2, 0, null,
                2, List.of(2, 4), new BigDecimal("3.0"), List.of(new CheckinFacts.TagCount("fear", 2)), List.of(), 9L);
        return new CheckinSummaryResponse("CHECKIN_SUMMARY", "2026-09", text, facts, List.of("Status: DONE."), List.of(12L), 9L, model);
    }

    @Test
    void returnsTheFactsAndTheDraftInOneShape() throws Exception {
        when(service.summarize("2026-09", true, true)).thenReturn(response("**What happened**\nA buy.",
                new CheckinSummaryResponse.ModelInfo(true, true, null, 3, 1)));
        call("{\"month\":\"2026-09\",\"includeModel\":true}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kind").value("CHECKIN_SUMMARY"))
                .andExpect(jsonPath("$.month").value("2026-09"))
                .andExpect(jsonPath("$.text").value("**What happened**\nA buy."))
                .andExpect(jsonPath("$.sources[0]").value(12))
                .andExpect(jsonPath("$.contributionId").value(9))
                .andExpect(jsonPath("$.factsLines[0]").value("Status: DONE."))
                .andExpect(jsonPath("$.facts.investedCents").value(30000))
                .andExpect(jsonPath("$.facts.daysAfterDue").value(4))
                .andExpect(jsonPath("$.facts.moodAverage").value(3.0))
                .andExpect(jsonPath("$.facts.tags[0].name").value("fear"))
                .andExpect(jsonPath("$.facts.monthEnd").doesNotExist())
                .andExpect(jsonPath("$.model.requested").value(true))
                .andExpect(jsonPath("$.model.entriesSent").value(3))
                .andExpect(jsonPath("$.model.entriesSkipped").value(1));
    }

    @Test
    void theDraftIsOffByDefaultAndTheJournalIsOnByDefault() throws Exception {
        when(service.summarize("2026-09", false, true)).thenReturn(response(null, CheckinSummaryResponse.ModelInfo.notRequested()));
        call("{\"month\":\"2026-09\"}").andExpect(status().isOk()).andExpect(jsonPath("$.text").doesNotExist())
                .andExpect(jsonPath("$.model.requested").value(false));
        verify(service).summarize("2026-09", false, true);
    }

    @Test
    void theFlagsAreForwarded() throws Exception {
        when(service.summarize("2026-09", true, false)).thenReturn(response(null, CheckinSummaryResponse.ModelInfo.notRun("why")));
        call("{\"month\":\"2026-09\",\"includeModel\":true,\"includeJournal\":false}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.model.reason").value("why"));
        verify(service).summarize("2026-09", true, false);
    }

    @Test
    void aMissingOrMalformedMonthIsA400AndNothingRuns() throws Exception {
        for (String body : new String[] {"{}", "{\"month\":\"\"}", "{\"month\":\"2026-9\"}", "{\"month\":\"September\"}",
                "{\"month\":\"2026-09-01\"}", "nope"}) {
            call(body).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(service);
    }

    @Test
    void aMonthTheServiceRefusesIsA400WithItsMessage() throws Exception {
        when(service.summarize("2026-13", false, true)).thenThrow(new BadRequestException("Choose a month like 2026-09"));
        call("{\"month\":\"2026-13\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Choose a month like 2026-09"));
    }
}
