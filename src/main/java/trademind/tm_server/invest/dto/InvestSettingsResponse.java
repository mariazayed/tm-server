package trademind.tm_server.invest.dto;

import trademind.tm_server.invest.InvestSettings;

import java.time.LocalDate;

public record InvestSettingsResponse(
        long monthlyAmountCents,
        String currency,
        int buyDay,
        String ticker,
        LocalDate startDate,
        String timezone,
        boolean remindersEnabled,
        String reminderEmail,
        int reminderFollowupDays,
        int ruleWaitHours) {

    public static InvestSettingsResponse from(InvestSettings s) {
        return new InvestSettingsResponse(s.getMonthlyAmountCents(), s.getCurrency(), s.getBuyDay(),
                s.getTicker(), s.getStartDate(), s.getTimezone(), s.isRemindersEnabled(),
                s.getReminderEmail(), s.getReminderFollowupDays(), s.getRuleWaitHours());
    }
}
