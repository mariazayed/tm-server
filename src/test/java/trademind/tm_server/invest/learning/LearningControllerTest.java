package trademind.tm_server.invest.learning;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.enums.SourceStatus;
import trademind.tm_server.enums.SourceType;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.InvestExceptionHandler;
import trademind.tm_server.invest.learning.LearningDtos.CardResponse;
import trademind.tm_server.invest.learning.LearningDtos.CardStats;
import trademind.tm_server.invest.learning.LearningDtos.SourceResponse;

import java.math.BigDecimal;
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
@ContextConfiguration(classes = LearningControllerTest.TestApp.class)
@Import({LearningController.class, GlobalExceptionHandler.class,
        InvestExceptionHandler.class})
class LearningControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean LearningService learning;
    @MockitoBean FlashcardService flashcards;

    private static CardResponse card() {
        return new CardResponse(3L, "What is DCA?", "Investing a fixed amount on a schedule.", null,
                LocalDate.parse("2026-10-07"), 6, new BigDecimal("2.50"), 2, 0, LocalDate.parse("2026-10-01"));
    }

    @Test
    void createsSourcesAndPassesTheStatusFilter() throws Exception {
        when(learning.createSource(any())).thenReturn(new SourceResponse(1L, "The Simple Path to Wealth",
                SourceType.BOOK, null, SourceStatus.TO_DO, null, LocalDateTime.parse("2026-10-06T09:00:00")));
        mvc.perform(post("/api/v1/invest/learning/sources").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"The Simple Path to Wealth\",\"sourceType\":\"BOOK\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("TO_DO"));

        when(learning.listSources(SourceStatus.DONE)).thenReturn(List.of());
        mvc.perform(get("/api/v1/invest/learning/sources").param("status", "DONE"))
                .andExpect(status().isOk());
        verify(learning).listSources(SourceStatus.DONE);
    }

    @Test
    void reviewPassesTheGradeAndReturnsTheNewSchedule() throws Exception {
        when(flashcards.review(eq(3L), eq(Sm2Scheduler.Grade.GOOD))).thenReturn(card());
        mvc.perform(post("/api/v1/invest/learning/cards/3/review")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"grade\":\"GOOD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalDays").value(6))
                .andExpect(jsonPath("$.dueOn").value("2026-10-07"));
    }

    @Test
    void invalidInputNeverReachesTheServices() throws Exception {
        for (String[] bad : new String[][] {
                {"cards/3/review", "{\"grade\":\"PERFECT\"}"},
                {"cards/3/review", "{}"},
                {"cards", "{\"front\":\"  \",\"back\":\"x\"}"},
                {"notes", "{\"title\":\"t\",\"body\":\"\"}"},
                {"sources", "{\"title\":\"t\"}"},
                {"sources", "{\"title\":\"t\",\"sourceType\":\"TELEPATHY\"}"}}) {
            mvc.perform(post("/api/v1/invest/learning/" + bad[0])
                    .contentType(MediaType.APPLICATION_JSON).content(bad[1])).andExpect(status().isBadRequest());
        }
        verify(flashcards, never()).review(any(), any());
        verify(flashcards, never()).create(any());
        verify(learning, never()).createNote(any());
        verify(learning, never()).createSource(any());
    }

    @Test
    void dueQueueAndStats() throws Exception {
        when(flashcards.due(5)).thenReturn(List.of(card()));
        when(flashcards.stats()).thenReturn(new CardStats(10, 4, 2, 3));
        mvc.perform(get("/api/v1/invest/learning/cards/due").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].front").value("What is DCA?"));
        mvc.perform(get("/api/v1/invest/learning/cards/stats"))
                .andExpect(jsonPath("$.due").value(4))
                .andExpect(jsonPath("$.newCards").value(2))
                .andExpect(jsonPath("$.reviewedToday").value(3));
    }

    @Test
    void noteFiltersArePassedThrough() throws Exception {
        mvc.perform(get("/api/v1/invest/learning/notes").param("sourceId", "4").param("tag", "dca")
                .param("q", "fixed").param("page", "1").param("size", "5")).andExpect(status().isOk());
        ArgumentCaptor<LearningService.NoteFilter> f = ArgumentCaptor.forClass(LearningService.NoteFilter.class);
        verify(learning).listNotes(f.capture(), eq(1), eq(5));
        assertEquals(4L, f.getValue().sourceId());
        assertEquals("dca", f.getValue().tag());
        assertEquals("fixed", f.getValue().query());
    }
}
