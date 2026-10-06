package trademind.tm_server.invest.journal;

import trademind.tm_server.invest.contribution.ContributionStatusCalculator.MonthState;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Currency;
import java.util.Locale;

/** Builds the markdown skeleton for the monthly check-in. Pure, so it is easy to test. */
public final class CheckinTemplateBuilder {

    private CheckinTemplateBuilder() {
    }

    public static String build(YearMonth month, MonthState state, int contributions, long totalCents,
                               String currency, int streak) {
        String monthName = month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
        return """
                ## Month check-in: %s

                **Contribution:** %s
                **Streak:** %d %s

                ### Did I stick to the plan?


                ### How do I feel about the market?


                ### What did I learn this month?


                ### Any changes to make? (default: none)
                None.
                """.formatted(monthName, describe(state, contributions, totalCents, currency), streak,
                streak == 1 ? "month" : "months");
    }

    static String describe(MonthState state, int contributions, long totalCents, String currency) {
        return switch (state) {
            case DONE -> "done (" + contributions + (contributions == 1 ? " buy, " : " buys, ")
                    + money(totalCents, currency) + ")";
            case PENDING -> "not yet bought this month";
            case OVERDUE -> "overdue, nothing bought yet this month";
            case MISSED -> "missed";
        };
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
