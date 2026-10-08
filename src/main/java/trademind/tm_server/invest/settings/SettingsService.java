package trademind.tm_server.invest.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.SettingsChange;
import trademind.tm_server.invest.dto.InvestSettingsResponse;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.SettingsChangeRepository;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class SettingsService {

    private final InvestSettingsRepository settings;
    private final SettingsChangeRepository changes;

    public SettingsService(InvestSettingsRepository settings, SettingsChangeRepository changes) {
        this.settings = settings;
        this.changes = changes;
    }

    public InvestSettingsResponse update(SettingsRequest r) {
        String currency = r.currency().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(currency);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown currency code: " + currency);
        }
        try {
            ZoneId.of(r.timezone());
        } catch (DateTimeException e) {
            throw new BadRequestException("Unknown timezone: " + r.timezone());
        }
        String email = r.reminderEmail() == null || r.reminderEmail().isBlank() ? null : r.reminderEmail().strip();
        if (r.remindersEnabled() && email == null) {
            throw new BadRequestException("Add an email address to turn reminders on");
        }

        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        String ticker = r.ticker().toUpperCase(Locale.ROOT);
        recordChanges(s, r, ticker);
        s.setMonthlyAmountCents(r.monthlyAmountCents());
        s.setCurrency(currency);
        s.setBuyDay(r.buyDay().shortValue());
        s.setTicker(ticker);
        s.setStartDate(r.startDate());
        s.setTimezone(r.timezone());
        s.setRemindersEnabled(r.remindersEnabled());
        s.setReminderEmail(email);
        s.setReminderFollowupDays(r.reminderFollowupDays().shortValue());
        if (r.ruleWaitHours() != null) s.setRuleWaitHours(r.ruleWaitHours());
        return InvestSettingsResponse.from(settings.save(s));
    }

    /**
     * Writes one history row per tracked field whose value really changes (saving the same plan again
     * writes nothing). The settings row is overwritten, so this is the only record of the old value.
     */
    private void recordChanges(InvestSettings current, SettingsRequest r, String newTicker) {
        List<SettingsChange> rows = new ArrayList<>();
        if (current.getMonthlyAmountCents() != r.monthlyAmountCents()) {
            rows.add(new SettingsChange(SettingsChange.MONTHLY_AMOUNT,
                    Long.toString(current.getMonthlyAmountCents()), Long.toString(r.monthlyAmountCents())));
        }
        if (!current.getTicker().equals(newTicker)) {
            rows.add(new SettingsChange(SettingsChange.TICKER, current.getTicker(), newTicker));
        }
        if (current.getBuyDay() != r.buyDay()) {
            rows.add(new SettingsChange(SettingsChange.BUY_DAY,
                    Short.toString(current.getBuyDay()), Integer.toString(r.buyDay())));
        }
        changes.saveAll(rows);
    }
}
