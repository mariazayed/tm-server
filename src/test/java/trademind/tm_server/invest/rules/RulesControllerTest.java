package trademind.tm_server.invest.rules;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.InvestExceptionHandler;
import trademind.tm_server.invest.dto.InvestSettingsResponse;
import trademind.tm_server.invest.reminder.ReminderKind;
import trademind.tm_server.invest.reminder.ReminderService;
import trademind.tm_server.invest.settings.SettingsRequest;
import trademind.tm_server.invest.settings.SettingsService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = RulesControllerTest.TestApp.class)
@Import({RulesController.class, GlobalExceptionHandler.class,
        InvestExceptionHandler.class})
class RulesControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean RulesService rules;
    @MockitoBean SettingsService settings;
    @MockitoBean ReminderService reminders;

    private static final String VALID_SETTINGS = """
            {"monthlyAmountCents":30000,"currency":"usd","buyDay":5,"ticker":"vt","startDate":"2026-08-01",
             "timezone":"Europe/Berlin","remindersEnabled":true,"reminderEmail":"me@example.com","reminderFollowupDays":3}""";

    private static InvestSettingsResponse settingsResponse() {
        return new InvestSettingsResponse(30000, "USD", 5, "VT", LocalDate.parse("2026-08-01"), "Europe/Berlin",
                true, "me@example.com", 3, 72);
    }

    @Test
    void rulesCrudAndOrder() throws Exception {
        when(rules.list()).thenReturn(List.of(new RulesService.RuleResponse(1L, "Never sell in a crash.", 1)));
        mvc.perform(get("/api/v1/invest/rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Never sell in a crash."))
                .andExpect(jsonPath("$[0].position").value(1));

        when(rules.create(any())).thenReturn(new RulesService.RuleResponse(2L, "Stay boring.", 2));
        mvc.perform(post("/api/v1/invest/rules").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"Stay boring.\"}"))
                .andExpect(status().isCreated());

        // "order" must not be mistaken for a rule id.
        when(rules.reorder(any())).thenReturn(List.of());
        mvc.perform(put("/api/v1/invest/rules/order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[2,1]}")).andExpect(status().isOk());
        ArgumentCaptor<RulesService.OrderRequest> order = ArgumentCaptor.forClass(RulesService.OrderRequest.class);
        verify(rules).reorder(order.capture());
        assertEquals(List.of(2L, 1L), order.getValue().ids());

        mvc.perform(delete("/api/v1/invest/rules/2")).andExpect(status().isNoContent());
        verify(rules).delete(2L);
    }

    @Test
    void invalidRuleInputNeverReachesTheService() throws Exception {
        for (String body : new String[] {"{\"text\":\"   \"}", "{}", "{\"text\":\"%s\"}".formatted("x".repeat(501))}) {
            mvc.perform(post("/api/v1/invest/rules").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(put("/api/v1/invest/rules/order").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ids\":[]}")).andExpect(status().isBadRequest());
        verify(rules, never()).create(any());
        verify(rules, never()).reorder(any());
    }

    @Test
    void settingsAreUpdatedThroughTheService() throws Exception {
        when(settings.update(any())).thenReturn(settingsResponse());
        mvc.perform(put("/api/v1/invest/settings").contentType(MediaType.APPLICATION_JSON).content(VALID_SETTINGS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.remindersEnabled").value(true))
                .andExpect(jsonPath("$.reminderFollowupDays").value(3));
        ArgumentCaptor<SettingsRequest> sent = ArgumentCaptor.forClass(SettingsRequest.class);
        verify(settings).update(sent.capture());
        assertEquals(5, sent.getValue().buyDay());
        assertEquals("Europe/Berlin", sent.getValue().timezone());
    }

    @Test
    void badSettingsAreRejectedBeforeTheService() throws Exception {
        String[] bad = {
                VALID_SETTINGS.replace("\"buyDay\":5", "\"buyDay\":0"),
                VALID_SETTINGS.replace("\"buyDay\":5", "\"buyDay\":29"),
                VALID_SETTINGS.replace("\"monthlyAmountCents\":30000", "\"monthlyAmountCents\":0"),
                VALID_SETTINGS.replace("\"currency\":\"usd\"", "\"currency\":\"dollars\""),
                VALID_SETTINGS.replace("\"ticker\":\"vt\"", "\"ticker\":\"v t!\""),
                VALID_SETTINGS.replace("me@example.com", "not-an-email"),
                VALID_SETTINGS.replace("\"reminderFollowupDays\":3", "\"reminderFollowupDays\":15"),
                VALID_SETTINGS.replace("\"timezone\":\"Europe/Berlin\",", ""),
        };
        for (String body : bad) {
            mvc.perform(put("/api/v1/invest/settings").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(settings, never()).update(any());
    }

    @Test
    void reminderStatusAndTestSend() throws Exception {
        when(reminders.status()).thenReturn(new ReminderService.Status(true,
                List.of(new ReminderService.Status.Sent("2026-10", ReminderKind.DUE, LocalDateTime.parse("2026-10-05T09:00:00")))));
        mvc.perform(get("/api/v1/invest/reminders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailConfigured").value(true))
                .andExpect(jsonPath("$.recent[0].kind").value("DUE"));

        when(reminders.sendTest()).thenReturn(new ReminderService.RunResult(false, null, null, "SMTP down"));
        mvc.perform(post("/api/v1/invest/reminders/test"))
                .andExpect(status().isOk()) // failure is in the body, not an HTTP error
                .andExpect(jsonPath("$.sent").value(false))
                .andExpect(jsonPath("$.error").value("SMTP down"));
    }
}
