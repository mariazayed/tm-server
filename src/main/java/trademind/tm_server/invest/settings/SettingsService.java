package trademind.tm_server.invest.settings;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.dto.InvestSettingsResponse;
import trademind.tm_server.invest.repository.InvestSettingsRepository;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;

@Service
@Transactional
public class SettingsService {

    private final InvestSettingsRepository settings;

    public SettingsService(InvestSettingsRepository settings) {
        this.settings = settings;
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
        s.setMonthlyAmountCents(r.monthlyAmountCents());
        s.setCurrency(currency);
        s.setBuyDay(r.buyDay().shortValue());
        s.setTicker(r.ticker().toUpperCase(Locale.ROOT));
        s.setStartDate(r.startDate());
        s.setTimezone(r.timezone());
        s.setRemindersEnabled(r.remindersEnabled());
        s.setReminderEmail(email);
        s.setReminderFollowupDays(r.reminderFollowupDays().shortValue());
        return InvestSettingsResponse.from(settings.save(s));
    }
}
