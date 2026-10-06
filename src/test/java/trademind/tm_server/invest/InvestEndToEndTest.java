package trademind.tm_server.invest;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.price.PriceProvider;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The whole investing flow over HTTP against a real PostgreSQL: the real security chain, JSON
 * converters, validation, services and SQL; only the price source is replaced (no network). It is one
 * story told in order (log in, record a buy, watch the numbers move, journal, learn, export), so the
 * steps share state and run in a fixed order. Needs a scratch database; see InvestSchemaTest.
 */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@SpringBootTest(classes = InvestEndToEndTest.TestApp.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "spring.datasource.url=${INVEST_TEST_DB_URL}",
        "spring.datasource.username=${INVEST_TEST_DB_USER:postgres}",
        "spring.datasource.password=${INVEST_TEST_DB_PASSWORD:}",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:invest-schema-reset.sql,"
                + "classpath:db/migration/V22__create_invest_tables.sql,"
                + "classpath:db/migration/V23__create_invest_flashcards.sql,"
                + "classpath:db/migration/V24__invest_reminders.sql",
        "app.auth.session-secret=0123456789abcdef0123456789abcdef-e2e",
        "app.max-body-bytes=2048"
})
class InvestEndToEndTest {

    static final String PASSWORD = "correct horse battery";

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    // Scan the production packages only: other test classes declare their own nested
    // @SpringBootConfiguration (each with its own repositories), which must not be picked up here.
    @ComponentScan(basePackages = {"trademind.tm_server.invest", "trademind.tm_server.auth"},
            excludeFilters = @ComponentScan.Filter(type = org.springframework.context.annotation.FilterType.ANNOTATION,
                    classes = org.springframework.boot.SpringBootConfiguration.class))
    @EntityScan(basePackageClasses = InvestSettings.class)
    @EnableJpaRepositories(basePackageClasses = InvestSettingsRepository.class)
    @Import(GlobalExceptionHandler.class)
    static class TestApp {
    }

    @DynamicPropertySource
    static void passwordHash(DynamicPropertyRegistry registry) {
        registry.add("app.auth.password-hash", () -> new BCryptPasswordEncoder(4).encode(PASSWORD));
    }

    @Autowired MockMvc mvc;
    @MockitoBean PriceProvider prices;

    // Shared across the ordered steps.
    private static String token;
    private static long contributionId;
    private static long sourceId;
    private static long noteId;
    private static long cardId;

    private static final LocalDate TODAY = LocalDate.now(ZoneOffset.UTC);

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer " + token);
    }

    private String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString();
    }

    @Test
    @Order(1)
    void nothingIsReachableWithoutLoggingIn() throws Exception {
        for (String path : new String[] {"/api/v1/invest/contributions", "/api/v1/invest/performance",
                "/api/v1/invest/journal", "/api/v1/invest/learning/cards/due", "/api/v1/invest/rules",
                "/api/v1/invest/export", "/api/v1/invest/settings", "/api/v1/auth/me"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/invest/contributions").header("Authorization", "Bearer forged.token.value"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(2)
    void aWrongPasswordIsRefusedAndTheRightOneYieldsAToken() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());

        MvcResult ok = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        token = JsonPath.read(body(ok), "$.token");

        mvc.perform(auth(get("/api/v1/auth/me"))).andExpect(status().isOk());
    }

    @Test
    @Order(3)
    void theSeededPlanAndRulesAreThere() throws Exception {
        mvc.perform(auth(get("/api/v1/invest/settings")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyAmountCents").value(30000))
                .andExpect(jsonPath("$.ticker").value("VT"));
        mvc.perform(auth(get("/api/v1/invest/rules")))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].text").value("Never sell in a crash."));
    }

    @Test
    @Order(4)
    void settingsCanBeChangedAndAreValidated() throws Exception {
        String plan = """
                {"monthlyAmountCents":30000,"currency":"USD","buyDay":%d,"ticker":"VT","startDate":"%s",
                 "timezone":"UTC","remindersEnabled":false,"reminderFollowupDays":3}""";
        mvc.perform(auth(put("/api/v1/invest/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content(plan.formatted(31, TODAY.minusMonths(2).withDayOfMonth(1))))
                .andExpect(status().isBadRequest()); // buy day is capped at 28
        mvc.perform(auth(put("/api/v1/invest/settings")).contentType(MediaType.APPLICATION_JSON)
                        .content(plan.formatted(1, TODAY.minusMonths(2).withDayOfMonth(1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.buyDay").value(1));
    }

    @Test
    @Order(5)
    void recordingABuyMovesTheStatusAndTheNumbers() throws Exception {
        MvcResult created = mvc.perform(auth(post("/api/v1/invest/contributions")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contributedOn":"%s","amountCents":30000,"shares":2.55489022,"price":117.42505,
                                 "notes":"first buy"}""".formatted(TODAY)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.price").value(117.4251)) // 117.42505 rounded half-up to the 4 decimals the database keeps
                .andReturn();
        contributionId = ((Number) JsonPath.read(body(created), "$.id")).longValue();

        mvc.perform(auth(get("/api/v1/invest/contributions/status")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.months[-1:].state").value("DONE"))
                .andExpect(jsonPath("$.currentStreak").value(1))
                .andExpect(jsonPath("$.missedCount").value(2)); // the plan started two months ago and nothing was bought then

        // No market prices cached yet: valued at what you paid, and it says so.
        mvc.perform(auth(get("/api/v1/invest/performance")))
                .andExpect(jsonPath("$.investedCents").value(30000))
                .andExpect(jsonPath("$.priceFromCache").value(false))
                .andExpect(jsonPath("$.gainCents", org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.greaterThanOrEqualTo(-5), org.hamcrest.Matchers.lessThanOrEqualTo(5)))); // rounding only
    }

    @Test
    @Order(6)
    void refreshingPricesRevaluesThePortfolio() throws Exception {
        when(prices.fetchDailyCloses(eq("VT"), any(), any())).thenReturn(List.of(
                new PriceProvider.DailyClose(TODAY, new BigDecimal("130.0000"))));

        mvc.perform(auth(post("/api/v1/invest/prices/refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inserted").value(1))
                .andExpect(jsonPath("$.error").doesNotExist());

        // 2.55489022 shares * $130 = $332.14
        mvc.perform(auth(get("/api/v1/invest/performance")))
                .andExpect(jsonPath("$.priceFromCache").value(true))
                .andExpect(jsonPath("$.valueCents").value(33214))
                .andExpect(jsonPath("$.gainCents").value(3214))
                .andExpect(jsonPath("$.series.length()").value(1));
    }

    @Test
    @Order(7)
    void aFailingPriceSourceKeepsTheLastPrices() throws Exception {
        when(prices.fetchDailyCloses(any(), any(), any()))
                .thenThrow(new PriceProvider.PriceProviderException("HTTP 429"));
        mvc.perform(auth(post("/api/v1/invest/prices/refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value("HTTP 429"));
        mvc.perform(auth(get("/api/v1/invest/performance")))
                .andExpect(jsonPath("$.valueCents").value(33214));
    }

    @Test
    @Order(8)
    void theMonthlyCheckinIsPrefilledAndLinkedToTheBuy() throws Exception {
        MvcResult template = mvc.perform(auth(get("/api/v1/invest/journal/checkin-template")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contributionId").value((int) contributionId))
                .andReturn();
        String markdown = JsonPath.read(body(template), "$.body");
        assertTrue(markdown.contains("**Contribution:** done (1 buy, $300.00)"), markdown);

        mvc.perform(auth(post("/api/v1/invest/journal")).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"entryType":"CHECKIN","mood":4,"body":%s,"tags":["Check-in","#Emotion"],
                                 "contributionId":%d}""".formatted(net.minidev.json.JSONValue.toJSONString(markdown), contributionId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags[0]").value("check-in"));

        mvc.perform(auth(get("/api/v1/invest/journal")).param("tag", "#EMOTION").param("q", "month check-in"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @Order(9)
    void learningNotesBecomeFlashcardsThatAreReviewed() throws Exception {
        sourceId = ((Number) JsonPath.read(body(mvc.perform(auth(post("/api/v1/invest/learning/sources"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"The Simple Path to Wealth\",\"sourceType\":\"BOOK\",\"url\":\"https://example.com\"}"))
                .andExpect(status().isCreated()).andReturn()), "$.id")).longValue();
        mvc.perform(auth(post("/api/v1/invest/learning/sources")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"x\",\"sourceType\":\"BOOK\",\"url\":\"javascript:alert(1)\"}"))
                .andExpect(status().isBadRequest());

        noteId = ((Number) JsonPath.read(body(mvc.perform(auth(post("/api/v1/invest/learning/notes"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"DCA\",\"body\":\"Invest a fixed amount on a schedule.\",\"sourceId\":%d,\"tags\":[\"basics\"]}"
                        .formatted(sourceId)))
                .andExpect(status().isCreated()).andReturn()), "$.id")).longValue();

        cardId = ((Number) JsonPath.read(body(mvc.perform(auth(post("/api/v1/invest/learning/cards"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"front\":\"What is DCA?\",\"back\":\"A fixed amount on a schedule.\",\"noteId\":%d}"
                        .formatted(noteId)))
                .andExpect(status().isCreated()).andReturn()), "$.id")).longValue();

        mvc.perform(auth(get("/api/v1/invest/learning/cards/due")))
                .andExpect(jsonPath("$.length()").value(1));
        mvc.perform(auth(post("/api/v1/invest/learning/cards/%d/review".formatted(cardId)))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"grade\":\"GOOD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intervalDays").value(1))
                .andExpect(jsonPath("$.dueOn").value(TODAY.plusDays(1).toString()));
        mvc.perform(auth(get("/api/v1/invest/learning/cards/due")))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(10)
    void rulesCanBeReordered() throws Exception {
        MvcResult rules = mvc.perform(auth(get("/api/v1/invest/rules"))).andReturn();
        List<Integer> ids = JsonPath.read(body(rules), "$[*].id");
        java.util.Collections.reverse(ids);
        mvc.perform(auth(put("/api/v1/invest/rules/order")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":" + ids + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].text").value("Wait 72 hours before any deviation."));
    }

    @Test
    @Order(11)
    void theExportHoldsEverythingAndNeverLeaksAcrossTheLoginWall() throws Exception {
        MvcResult export = mvc.perform(auth(get("/api/v1/invest/export")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("invest-export-")))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.schemaVersion").value(1))
                .andExpect(jsonPath("$.settings.ticker").value("VT"))
                .andExpect(jsonPath("$.contributions.length()").value(1))
                .andExpect(jsonPath("$.contributions[0].amountCents").value(30000))
                .andExpect(jsonPath("$.prices.length()").value(1))
                .andExpect(jsonPath("$.journal.length()").value(1))
                .andExpect(jsonPath("$.journal[0].contributionId").value((int) contributionId))
                .andExpect(jsonPath("$.sources.length()").value(1))
                .andExpect(jsonPath("$.notes[0].sourceId").value((int) sourceId))
                .andExpect(jsonPath("$.flashcards[0].noteId").value((int) noteId))
                .andExpect(jsonPath("$.rules.length()").value(3))
                .andReturn();
        assertTrue(body(export).contains("Invest a fixed amount on a schedule."));

        mvc.perform(get("/api/v1/invest/export")).andExpect(status().isUnauthorized());
    }

    @Test
    @Order(12)
    void badInputIsRefusedCleanly() throws Exception {
        // Malformed JSON is a client error, not a 500.
        mvc.perform(auth(post("/api/v1/invest/journal")).contentType(MediaType.APPLICATION_JSON).content("{nope"))
                .andExpect(status().isBadRequest());
        // An unrealistic amount never reaches the database.
        mvc.perform(auth(post("/api/v1/invest/contributions")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contributedOn\":\"%s\",\"amountCents\":99999999999999,\"shares\":1,\"price\":1}".formatted(TODAY)))
                .andExpect(status().isBadRequest());
        // A future date is refused by the business rule.
        mvc.perform(auth(post("/api/v1/invest/contributions")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contributedOn\":\"%s\",\"amountCents\":30000,\"shares\":2.55,\"price\":117.6}"
                                .formatted(TODAY.plusDays(2))))
                .andExpect(status().isBadRequest());
        // A body over the size limit is refused with 413 before it is read into memory.
        mvc.perform(auth(post("/api/v1/invest/contributions/import")).contentType("text/csv").content("x".repeat(5000)))
                .andExpect(status().isPayloadTooLarge());
        // CSV with an impossible row is reported per line, nothing is saved.
        mvc.perform(auth(post("/api/v1/invest/contributions/import")).contentType("text/csv")
                        .content("date,amount,shares,price\n%s,300,99999999999999999,1\n".formatted(TODAY)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].line").value(2));
    }

    @Test
    @Order(13)
    void deletingTheBuyUpdatesEverythingAndTheExport() throws Exception {
        mvc.perform(auth(delete("/api/v1/invest/contributions/" + contributionId))).andExpect(status().isNoContent());
        mvc.perform(auth(get("/api/v1/invest/performance")))
                .andExpect(jsonPath("$.investedCents").value(0))
                .andExpect(jsonPath("$.valueCents").value(0));
        mvc.perform(auth(get("/api/v1/invest/export")))
                .andExpect(jsonPath("$.contributions.length()").value(0))
                .andExpect(jsonPath("$.journal[0].contributionId").doesNotExist()); // link to a deleted buy is not exported
        assertEquals(0, ((List<?>) JsonPath.read(body(mvc.perform(auth(get("/api/v1/invest/contributions"))).andReturn()), "$")).size());
    }
}
