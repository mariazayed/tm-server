package trademind.tm_server.invest.checkin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Everything numeric about one month, computed by code from the owner's own data. This is what the app
 * shows next to a drafted summary, and it is the only source of numbers the model is allowed to use: a
 * number in the prose that is not here (or in the owner's own words) is rejected.
 *
 * @param plannedCents     the monthly amount in the plan now (the plan keeps no per-month history of it)
 * @param state            DONE, MISSED, PENDING or OVERDUE (the last two only for the current month)
 * @param investedCents    the amount bought this month, fees not included
 * @param averagePrice     amount bought divided by shares bought, 4 decimals; null with no buys
 * @param daysAfterDue     days from the buy day to the first buy; negative means early; null with no buys
 * @param monthEnd         value at the end of the month (or today, for the current month); null when there is no
 *                         cached market price on or before that date
 * @param moods            the mood (1 worried to 5 calm) of each entry that has one, oldest first
 * @param moodAverage      one decimal; null with no moods
 * @param ruleNotes        messages of the exact consistency checks for this month
 * @param contributionId   the month's latest buy, so a check-in entry can link to it; null with no buys
 */
public record CheckinFacts(
        String month,
        String currency,
        long plannedCents,
        int buyDay,
        LocalDate dueDate,
        String state,
        List<Buy> buys,
        long investedCents,
        long feesCents,
        BigDecimal sharesBought,
        BigDecimal averagePrice,
        LocalDate firstBuyDate,
        Integer daysAfterDue,
        int currentStreak,
        int longestStreak,
        int missedMonths,
        MonthEndValue monthEnd,
        int entryCount,
        List<Integer> moods,
        BigDecimal moodAverage,
        List<TagCount> tags,
        List<String> ruleNotes,
        Long contributionId) {

    public record Buy(LocalDate date, long amountCents, BigDecimal shares, BigDecimal price, long feeCents) {
    }

    /**
     * @param asOf               the date of the market price used
     * @param investedToDateCents everything bought up to the end of the month, fees included
     */
    public record MonthEndValue(LocalDate asOf, long investedToDateCents, long valueCents, long gainCents,
                                BigDecimal gainPercent) {
    }

    public record TagCount(String name, int count) {
    }
}
