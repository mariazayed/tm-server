package trademind.tm_server.invest.reminder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.dto.ContributionRequest;
import trademind.tm_server.invest.dto.InvestSettingsResponse;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.rules.RulesService;
import trademind.tm_server.invest.rules.RulesService.OrderRequest;
import trademind.tm_server.invest.rules.RulesService.RuleRequest;
import trademind.tm_server.invest.rules.RulesService.RuleResponse;
import trademind.tm_server.invest.settings.SettingsRequest;
import trademind.tm_server.invest.settings.SettingsService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Needs a scratch PostgreSQL; see InvestSchemaTest. The clock is movable; it starts 2026-10-15 UTC. */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = RulesSettingsReminderServiceTest.TestApp.class)
@Import({RulesService.class, SettingsService.class, ReminderService.class, ContributionService.class,
        RulesSettingsReminderServiceTest.Fakes.class})
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
                + "classpath:db/migration/V24__invest_reminders.sql,"
                + "classpath:db/migration/V25__invest_embeddings.sql"
})
class RulesSettingsReminderServiceTest {

    @org.springframework.boot.SpringBootConfiguration
    @org.springframework.boot.autoconfigure.EnableAutoConfiguration
    @EntityScan(basePackageClasses = {InvestSettings.class, ReminderLog.class})
    @EnableJpaRepositories(basePackageClasses = InvestSettingsRepository.class)
    static class TestApp {
    }

    /** A clock the test can move between runs. */
    static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-10-15T12:00:00Z");

        void set(String isoInstant) {
            now = Instant.parse(isoInstant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    record Mail(String to, String subject, String body) {
    }

    static class FakeSender implements ReminderSender {
        final List<Mail> sent = new ArrayList<>();
        boolean configured = true;
        String failWith;

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public void send(String to, String subject, String body) {
            if (failWith != null) throw new ReminderException(failWith);
            sent.add(new Mail(to, subject, body));
        }
    }

    static class Fakes {
        @Bean
        MutableClock clock() {
            return new MutableClock();
        }

        @Bean
        FakeSender sender() {
            return new FakeSender();
        }
    }

    @Autowired RulesService rules;
    @Autowired SettingsService settingsService;
    @Autowired ReminderService reminders;
    @Autowired ContributionService contributions;
    @Autowired InvestSettingsRepository settings;
    @Autowired MutableClock clock;
    @Autowired FakeSender sender;

    @BeforeEach
    void reset() {
        clock.set("2026-10-15T12:00:00Z");
        sender.sent.clear();
        sender.failWith = null;
        sender.configured = true;
    }

    private static SettingsRequest request(boolean remindersOn, String email, int buyDay) {
        return new SettingsRequest(30000L, "usd", buyDay, "vt", LocalDate.parse("2026-08-01"), "UTC",
                remindersOn, email, 3);
    }

    private void remindersOn(int buyDay) {
        settingsService.update(request(true, "me@example.com", buyDay));
    }

    // ---- rules --------------------------------------------------------------------------------

    @Test
    void migrationSeedsThreeRulesInOrder() {
        List<RuleResponse> seeded = rules.list();
        assertEquals(3, seeded.size());
        assertEquals("Never sell in a crash.", seeded.get(0).text());
        assertEquals(List.of(1, 2, 3), seeded.stream().map(RuleResponse::position).toList());
    }

    @Test
    void ruleLifecycle() {
        RuleResponse added = rules.create(new RuleRequest("  Stay boring.  "));
        assertEquals("Stay boring.", added.text());
        assertEquals(4, added.position());

        RuleResponse edited = rules.update(added.id(), new RuleRequest("Stay boring, forever."));
        assertEquals("Stay boring, forever.", edited.text());

        rules.delete(added.id());
        assertEquals(3, rules.list().size());
        assertThrows(ResourceNotFoundException.class, () -> rules.delete(added.id()));
        assertThrows(ResourceNotFoundException.class, () -> rules.update(added.id(), new RuleRequest("x")));
    }

    @Test
    void reorderingRenumbersAndRejectsIncompleteOrders() {
        List<RuleResponse> before = rules.list();
        List<Long> reversed = new ArrayList<>(before.stream().map(RuleResponse::id).toList());
        java.util.Collections.reverse(reversed);

        List<RuleResponse> after = rules.reorder(new OrderRequest(reversed));
        assertEquals(reversed, after.stream().map(RuleResponse::id).toList());
        assertEquals(List.of(1, 2, 3), after.stream().map(RuleResponse::position).toList());

        assertThrows(BadRequestException.class, () -> rules.reorder(new OrderRequest(reversed.subList(0, 2))));
        assertThrows(BadRequestException.class, () -> rules.reorder(new OrderRequest(List.of(reversed.get(0), reversed.get(0), reversed.get(1)))));
        assertThrows(BadRequestException.class, () -> rules.reorder(new OrderRequest(List.of(reversed.get(0), reversed.get(1), 99999L))));
    }

    @Test
    void newRulesGoAfterTheLastEvenAfterADelete() {
        List<RuleResponse> seeded = rules.list();
        rules.delete(seeded.get(1).id());
        RuleResponse added = rules.create(new RuleRequest("New one"));
        List<RuleResponse> all = rules.list();
        assertEquals(added.id(), all.get(all.size() - 1).id());
    }

    @Test
    void ruleCountIsCapped() {
        for (int i = rules.list().size(); i < RulesService.MAX_RULES; i++) {
            rules.create(new RuleRequest("rule " + i));
        }
        assertThrows(BadRequestException.class, () -> rules.create(new RuleRequest("one too many")));
    }

    // ---- settings -----------------------------------------------------------------------------

    @Test
    void settingsUpdateNormalisesAndPersistsEverything() {
        InvestSettingsResponse saved = settingsService.update(new SettingsRequest(45000L, "eur", 10, "vwce.de",
                LocalDate.parse("2026-01-01"), "Europe/Berlin", true, " me@example.com ", 5));
        assertEquals(45000, saved.monthlyAmountCents());
        assertEquals("EUR", saved.currency());
        assertEquals("VWCE.DE", saved.ticker());
        assertEquals(10, saved.buyDay());
        assertEquals("me@example.com", saved.reminderEmail());
        assertEquals(5, saved.reminderFollowupDays());

        InvestSettings stored = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        assertEquals("Europe/Berlin", stored.getTimezone());
        assertTrue(stored.isRemindersEnabled());
    }

    @Test
    void settingsRejectUnknownCurrencyTimezoneAndReminderWithoutEmail() {
        assertThrows(BadRequestException.class, () -> settingsService.update(
                new SettingsRequest(30000L, "ZZZ", 1, "VT", LocalDate.parse("2026-08-01"), "UTC", false, null, 3)));
        assertThrows(BadRequestException.class, () -> settingsService.update(
                new SettingsRequest(30000L, "USD", 1, "VT", LocalDate.parse("2026-08-01"), "Mars/Olympus", false, null, 3)));
        BadRequestException e = assertThrows(BadRequestException.class, () -> settingsService.update(
                new SettingsRequest(30000L, "USD", 1, "VT", LocalDate.parse("2026-08-01"), "UTC", true, "  ", 3)));
        assertTrue(e.getMessage().contains("email"));
        // Nothing was half-saved.
        assertEquals("USD", settings.findById(InvestSettings.SINGLETON_ID).orElseThrow().getCurrency());
    }

    @Test
    void remindersCanBeOffWithoutAnEmail() {
        InvestSettingsResponse saved = settingsService.update(request(false, null, 1));
        assertFalse(saved.remindersEnabled());
        assertNull(saved.reminderEmail());
    }

    // ---- reminders ----------------------------------------------------------------------------

    @Test
    void nothingIsSentWhileRemindersAreOffOrHaveNoAddress() {
        assertEquals("Reminders are turned off", reminders.runDaily().skipped());
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        s.setRemindersEnabled(true);
        settings.saveAndFlush(s);
        assertEquals("No reminder email address set", reminders.runDaily().skipped());
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    void sendsTheDueReminderOnceOnTheBuyDay() {
        remindersOn(15);
        ReminderService.RunResult first = reminders.runDaily();
        assertTrue(first.sent());
        assertEquals(ReminderKind.DUE, first.kind());
        assertEquals(1, sender.sent.size());
        Mail mail = sender.sent.get(0);
        assertEquals("me@example.com", mail.to());
        assertTrue(mail.subject().contains("VT"));
        assertTrue(mail.body().contains("$300.00"));
        assertTrue(mail.body().contains("Never sell in a crash.")); // your rules ride along

        assertFalse(reminders.runDaily().sent()); // second run the same day: nothing new
        assertEquals(1, sender.sent.size());
    }

    @Test
    void followsUpThreeDaysLaterThenStaysQuiet() {
        remindersOn(15);
        reminders.runDaily(); // DUE on the 15th
        clock.set("2026-10-17T12:00:00Z");
        assertFalse(reminders.runDaily().sent());
        clock.set("2026-10-18T12:00:00Z");
        ReminderService.RunResult followup = reminders.runDaily();
        assertEquals(ReminderKind.FOLLOWUP, followup.kind());
        assertTrue(sender.sent.get(1).subject().contains("still pending"));
        clock.set("2026-10-25T12:00:00Z");
        assertFalse(reminders.runDaily().sent());
        assertEquals(2, sender.sent.size());
    }

    @Test
    void contributingBeforeTheBuyDaySilencesTheMonth() {
        remindersOn(15);
        contributions.create(new ContributionRequest(LocalDate.parse("2026-10-02"), 30000L,
                new BigDecimal("2.5549"), new BigDecimal("117.425"), null, null));
        ReminderService.RunResult r = reminders.runDaily();
        assertFalse(r.sent());
        assertEquals("Already contributed this month", r.skipped());
        assertTrue(sender.sent.isEmpty());
    }

    @Test
    void contributingAfterTheDueReminderCancelsTheFollowup() {
        remindersOn(15);
        reminders.runDaily();
        contributions.create(new ContributionRequest(LocalDate.parse("2026-10-15"), 30000L,
                new BigDecimal("2.5549"), new BigDecimal("117.425"), null, null));
        clock.set("2026-10-18T12:00:00Z");
        assertFalse(reminders.runDaily().sent());
        assertEquals(1, sender.sent.size());
    }

    @Test
    void aFailedSendIsReportedAndRetriedNextRun() {
        remindersOn(15);
        sender.failWith = "SMTP down";
        ReminderService.RunResult failed = reminders.runDaily();
        assertFalse(failed.sent());
        assertEquals("SMTP down", failed.error());
        assertEquals(ReminderKind.DUE, failed.kind());
        assertTrue(reminders.status().recent().isEmpty()); // nothing logged, so it can retry

        sender.failWith = null;
        assertTrue(reminders.runDaily().sent());
        assertEquals(1, reminders.status().recent().size());
    }

    @Test
    void anOverdueMonthStartsFreshTheNextMonth() {
        remindersOn(15);
        reminders.runDaily();
        clock.set("2026-11-15T12:00:00Z");
        ReminderService.RunResult november = reminders.runDaily();
        assertTrue(november.sent());
        assertEquals(ReminderKind.DUE, november.kind());
    }

    @Test
    void testMessageIgnoresTheSchedule() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        assertEquals("Save a reminder email address first", reminders.sendTest().skipped());

        settingsService.update(request(false, "me@example.com", 28)); // reminders off, not due
        assertTrue(reminders.sendTest().sent());
        assertEquals("Invest tracker: test reminder", sender.sent.get(0).subject());
        assertNotNull(s);

        sender.failWith = "no route to host";
        ReminderService.RunResult failed = reminders.sendTest();
        assertFalse(failed.sent());
        assertEquals("no route to host", failed.error());
    }

    @Test
    void statusReportsTheChannelAndRecentReminders() {
        assertTrue(reminders.status().emailConfigured());
        sender.configured = false;
        assertFalse(reminders.status().emailConfigured());
        remindersOn(15);
        reminders.runDaily();
        assertEquals("2026-10", reminders.status().recent().get(0).month());
    }
}
