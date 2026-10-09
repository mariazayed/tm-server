package trademind.tm_server.invest.journal;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.enums.JournalEntryType;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.InvestExceptionHandler;
import trademind.tm_server.invest.journal.JournalDtos.EntryRequest;
import trademind.tm_server.invest.journal.JournalDtos.EntryResponse;
import trademind.tm_server.invest.journal.JournalDtos.PageResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = JournalControllerTest.TestApp.class)
@Import({JournalController.class, GlobalExceptionHandler.class,
        InvestExceptionHandler.class})
class JournalControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean JournalService service;

    private static final String VALID = """
            {"entryType":"QUICK","mood":2,"body":"Headlines are loud.","tags":["emotion"]}""";

    private static EntryResponse entry() {
        return new EntryResponse(5L, JournalEntryType.QUICK, (short) 2, "Headlines are loud.", List.of("emotion"),
                null, LocalDateTime.parse("2026-10-06T09:30:00"), null);
    }

    @Test
    void timelinePassesFiltersAndPagingToTheService() throws Exception {
        when(service.list(any(), eq(1), eq(10))).thenReturn(new PageResponse(List.of(entry()), 1, 10, 11, false));

        mvc.perform(get("/api/v1/invest/journal").param("tag", "#Emotion").param("type", "QUICK").param("q", "loud")
                        .param("from", "2026-10-01").param("to", "2026-10-31").param("page", "1").param("size", "10")
                        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].body").value("Headlines are loud."))
                .andExpect(jsonPath("$.items[0].tags[0]").value("emotion"))
                .andExpect(jsonPath("$.total").value(11))
                .andExpect(jsonPath("$.hasMore").value(false));

        ArgumentCaptor<JournalService.Filter> filter = ArgumentCaptor.forClass(JournalService.Filter.class);
        verify(service).list(filter.capture(), eq(1), eq(10));
        assertEquals("#Emotion", filter.getValue().tag());
        assertEquals(JournalEntryType.QUICK, filter.getValue().type());
        assertEquals("loud", filter.getValue().query());
        assertEquals(LocalDate.parse("2026-10-01"), filter.getValue().from());
        assertEquals(LocalDate.parse("2026-10-31"), filter.getValue().to());
    }

    @Test
    void createReturns201() throws Exception {
        when(service.create(any())).thenReturn(entry());
        mvc.perform(post("/api/v1/invest/journal").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5));
        ArgumentCaptor<EntryRequest> req = ArgumentCaptor.forClass(EntryRequest.class);
        verify(service).create(req.capture());
        assertEquals(List.of("emotion"), req.getValue().tags());
    }

    @Test
    void invalidBodiesNeverReachTheService() throws Exception {
        for (String bad : new String[] {
                """
                {"entryType":"QUICK","body":"   "}""",
                """
                {"entryType":"QUICK","mood":6,"body":"x"}""",
                """
                {"entryType":"QUICK","mood":0,"body":"x"}""",
                """
                {"body":"no type"}""",
                """
                {"entryType":"QUICK","body":"x","tags":["%s"]}""".formatted("t".repeat(65)),
                """
                {"entryType":"NOPE","body":"x"}"""}) {
            mvc.perform(post("/api/v1/invest/journal").contentType(MediaType.APPLICATION_JSON).content(bad))
                    .andExpect(status().isBadRequest());
        }
        verify(service, never()).create(any());
    }

    @Test
    void deleteReturns204() throws Exception {
        mvc.perform(delete("/api/v1/invest/journal/5")).andExpect(status().isNoContent());
        verify(service).delete(5L);
    }
}
