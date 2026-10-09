package trademind.tm_server.invest.contribution;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.dto.ContributionResponse;
import trademind.tm_server.invest.dto.ImportResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = ContributionControllerTest.TestApp.class)
@Import({ContributionController.class, GlobalExceptionHandler.class,
        trademind.tm_server.invest.InvestExceptionHandler.class})
class ContributionControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean ContributionService service;

    private static final String VALID = """
            {"contributedOn":"2026-10-01","amountCents":30000,"shares":2.55489022,"price":117.425}""";

    @Test
    void listsContributions() throws Exception {
        when(service.list()).thenReturn(List.of(new ContributionResponse(1L, LocalDate.parse("2026-10-01"),
                30000, new BigDecimal("2.55489022"), new BigDecimal("117.4250"), 0, null)));
        mvc.perform(get("/api/v1/invest/contributions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amountCents").value(30000))
                .andExpect(jsonPath("$[0].contributedOn").value("2026-10-01"));
    }

    @Test
    void createReturns201() throws Exception {
        when(service.create(any())).thenReturn(new ContributionResponse(7L, LocalDate.parse("2026-10-01"),
                30000, new BigDecimal("2.55489022"), new BigDecimal("117.4250"), 0, null));
        mvc.perform(post("/api/v1/invest/contributions")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void invalidBodyIsRejectedBeforeReachingTheService() throws Exception {
        mvc.perform(post("/api/v1/invest/contributions").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contributedOn":"2026-10-01","amountCents":0,"shares":1,"price":1}"""))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    @Test
    void malformedJsonIs400NotAServerError() throws Exception {
        mvc.perform(post("/api/v1/invest/contributions").contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());
        verify(service, never()).create(any());
    }

    @Test
    void unrealisticValuesAreRejectedBeforeTheService() throws Exception {
        String[] bad = {
                // amount over $1 billion
                """
                {"contributedOn":"2026-10-01","amountCents":100000000001,"shares":1,"price":1}""",
                // shares that would overflow numeric(20,8)
                """
                {"contributedOn":"2026-10-01","amountCents":100,"shares":1000000000000,"price":1}""",
                // price that would overflow numeric(14,4)
                """
                {"contributedOn":"2026-10-01","amountCents":100,"shares":1,"price":10000000000}""",
                // absurd fee
                """
                {"contributedOn":"2026-10-01","amountCents":100,"shares":1,"price":1,"feeCents":100000001}""",
                // absurd number of decimals
                """
                {"contributedOn":"2026-10-01","amountCents":100,"shares":0.1234567890123456789012345,"price":1}"""};
        for (String body : bad) {
            mvc.perform(post("/api/v1/invest/contributions").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(service, never()).create(any());
    }

    @Test
    void importWithErrorsIs422AndSuccessIs200() throws Exception {
        when(service.importCsv("bad")).thenReturn(
                new ImportResult(0, 0, List.of(new ImportResult.RowError(2, "Date must be YYYY-MM-DD"))));
        when(service.importCsv("good")).thenReturn(new ImportResult(2, 1, List.of()));

        mvc.perform(post("/api/v1/invest/contributions/import").contentType("text/csv").content("bad"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].line").value(2));
        mvc.perform(post("/api/v1/invest/contributions/import").contentType("text/csv").content("good"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(2))
                .andExpect(jsonPath("$.skipped").value(1));
    }

    @Test
    void exportIsCsvDownload() throws Exception {
        when(service.exportCsv()).thenReturn("date,amount\n");
        mvc.perform(get("/api/v1/invest/contributions/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(content().string("date,amount\n"));
    }
}
