package trademind.tm_server.invest.reminder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.Rule;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.contribution.ContributionStatusCalculator.MonthState;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.ReminderLogRepository;
import trademind.tm_server.invest.repository.RuleRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Currency;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ReminderService {

    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

    /** Outcome of one run; {@code kind} is null when nothing was due. */
    public record RunResult(boolean sent, ReminderKind kind, String skipped, String error) {
        static RunResult skipped(String why) {
            return new RunResult(false, null, why, null);
        }
    }

    /** What the settings page shows about the reminder channel. */
    public record Status(boolean emailConfigured, List<Sent> recent) {
        public record Sent(String month, ReminderKind kind, LocalDateTime sentAt) {
        }
    }

    private final InvestSettingsRepository settings;
    private final ReminderLogRepository reminderLog;
    private final RuleRepository rules;
    private final ContributionService contributions;
    private final ReminderSender sender;
    private final Clock clock;

    public ReminderService(InvestSettingsRepository settings, ReminderLogRepository reminderLog,
                           RuleRepository rules, ContributionService contributions, ReminderSender sender,
                           Clock clock) {
        this.settings = settings;
        this.reminderLog = reminderLog;
        this.rules = rules;
        this.contributions = contributions;
        this.sender = sender;
        this.clock = clock;
    }

    /** Sends today's reminder if one is due. Safe to run any number of times a day. */
    @Transactional
    public RunResult runDaily() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        if (!s.isRemindersEnabled()) return RunResult.skipped("Reminders are turned off");
        if (s.getReminderEmail() == null || s.getReminderEmail().isBlank()) {
            return RunResult.skipped("No reminder email address set");
        }
        LocalDate today = LocalDate.now(clock.withZone(ZoneId.of(s.getTimezone())));
        String month = YearMonth.from(today).toString();

        var status = contributions.status();
        boolean monthDone = !status.months().isEmpty()
                && status.months().get(status.months().size() - 1).state() == MonthState.DONE;
        Set<ReminderKind> sent = EnumSet.noneOf(ReminderKind.class);
        reminderLog.findByMonth(month).forEach(l -> sent.add(l.getKind()));

        var next = ReminderPlanner.next(today, s.getBuyDay(), s.getReminderFollowupDays(), monthDone, sent);
        if (next.isEmpty()) return RunResult.skipped(monthDone ? "Already contributed this month" : "Nothing due today");

        ReminderKind kind = next.get();
        try {
            sender.send(s.getReminderEmail(), subject(kind, s), body(kind, s, today));
        } catch (ReminderSender.ReminderException e) {
            log.warn("Reminder email failed, will retry on the next run: {}", e.getMessage());
            return new RunResult(false, kind, null, e.getMessage());
        }
        try {
            reminderLog.saveAndFlush(new ReminderLog(month, kind, LocalDateTime.now(clock)));
        } catch (DataIntegrityViolationException e) {
            log.info("Reminder {} for {} was already logged by another run", kind, month);
        }
        return new RunResult(true, kind, null, null);
    }

    /** Sends a one-off message so you can check the channel works; not logged, ignores the schedule. */
    public RunResult sendTest() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        if (s.getReminderEmail() == null || s.getReminderEmail().isBlank()) {
            return RunResult.skipped("Save a reminder email address first");
        }
        try {
            sender.send(s.getReminderEmail(), "Invest tracker: test reminder",
                    "This is a test. If you can read it, your " + s.getTicker() + " reminders will arrive here.");
            return new RunResult(true, null, null, null);
        } catch (ReminderSender.ReminderException e) {
            return new RunResult(false, null, null, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Status status() {
        return new Status(sender.isConfigured(), reminderLog.findTop5ByOrderBySentAtDesc().stream()
                .map(l -> new Status.Sent(l.getMonth(), l.getKind(), l.getSentAt())).toList());
    }

    // ---- message text (no personal data beyond the plan itself) -------------------------------

    static String subject(ReminderKind kind, InvestSettings s) {
        return kind == ReminderKind.DUE
                ? "Time for your monthly " + s.getTicker() + " contribution"
                : "Reminder: your " + s.getTicker() + " contribution is still pending";
    }

    String body(ReminderKind kind, InvestSettings s, LocalDate today) {
        String amount = money(s.getMonthlyAmountCents(), s.getCurrency());
        StringBuilder b = new StringBuilder();
        b.append(kind == ReminderKind.DUE
                ? "Your monthly contribution of " + amount + " into " + s.getTicker() + " is due.\n"
                : "You have not logged this month's " + amount + " " + s.getTicker() + " contribution yet.\n");
        b.append("Buy, then record it in the app. Once it is logged you will not hear from us again this month.\n");
        List<Rule> live = rules.findByDeletedAtIsNullOrderBySortOrderAsc();
        if (!live.isEmpty()) {
            b.append("\nYour rules:\n");
            b.append(live.stream().map(r -> "- " + r.getText()).collect(Collectors.joining("\n"))).append('\n');
        }
        return b.toString();
    }

    static String money(long cents, String currency) {
        NumberFormat f = NumberFormat.getCurrencyInstance(Locale.US);
        try {
            f.setCurrency(Currency.getInstance(currency));
        } catch (IllegalArgumentException e) {
            // unknown code: keep the default symbol
        }
        return f.format(BigDecimal.valueOf(cents).movePointLeft(2));
    }
}
