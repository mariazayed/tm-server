package trademind.tm_server.invest.settings;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * The whole plan, replaced in one go.
 *
 * @param buyDay            1-28, so every month has it
 * @param reminderEmail     required when reminders are on
 * @param reminderFollowupDays days after the buy day to nudge again
 * @param ruleWaitHours     hours to wait before a deviation, 0-720 (0 turns the wait-period check off);
 *                          left out means keep the current value
 */
public record SettingsRequest(
        @NotNull @Positive @Max(100_000_000_00L) Long monthlyAmountCents,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        @NotNull @Min(1) @Max(28) Integer buyDay,
        @NotBlank @Pattern(regexp = "[A-Za-z0-9.\\-]{1,16}") String ticker,
        @NotNull LocalDate startDate,
        @NotBlank @Size(max = 64) String timezone,
        boolean remindersEnabled,
        @Email @Size(max = 254) String reminderEmail,
        @NotNull @Min(1) @Max(14) Integer reminderFollowupDays,
        @Min(0) @Max(720) Integer ruleWaitHours) {

    /** The request as clients sent it before {@code ruleWaitHours} existed: the current value is kept. */
    public SettingsRequest(Long monthlyAmountCents, String currency, Integer buyDay, String ticker, LocalDate startDate,
                           String timezone, boolean remindersEnabled, String reminderEmail, Integer reminderFollowupDays) {
        this(monthlyAmountCents, currency, buyDay, ticker, startDate, timezone, remindersEnabled, reminderEmail,
                reminderFollowupDays, null);
    }
}
