package trademind.tm_server.invest.backup;

import trademind.tm_server.enums.JournalEntryType;
import trademind.tm_server.enums.SourceStatus;
import trademind.tm_server.enums.SourceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Everything the investing tracker stores, as plain data. Soft-deleted rows are left out. Money is
 * integer cents, exactly as in the database. {@code schemaVersion} changes if a field is removed or
 * renamed, so a future restore tool knows what it is reading.
 */
public record ExportData(
        int schemaVersion,
        Instant exportedAt,
        Settings settings,
        List<Contribution> contributions,
        List<Price> prices,
        List<Tag> tags,
        List<JournalEntry> journal,
        List<Rule> rules,
        List<Source> sources,
        List<Note> notes,
        List<Flashcard> flashcards,
        List<Reminder> reminders,
        List<SettingsChange> settingsChanges) {

    public static final int SCHEMA_VERSION = 1;

    public record Settings(long monthlyAmountCents, String currency, int buyDay, String ticker, LocalDate startDate,
                           String timezone, boolean remindersEnabled, String reminderEmail, int reminderFollowupDays,
                           int ruleWaitHours) {
    }

    public record Contribution(Long id, LocalDate contributedOn, long amountCents, BigDecimal shares, BigDecimal price,
                               long feeCents, String notes, LocalDateTime createdAt) {
    }

    public record Price(String ticker, LocalDate date, BigDecimal close) {
    }

    public record Tag(String name) {
    }

    public record JournalEntry(Long id, JournalEntryType type, Short mood, String body, List<String> tags,
                               Long contributionId, LocalDateTime createdAt) {
    }

    public record Rule(Long id, String text, int position) {
    }

    public record Source(Long id, String title, SourceType type, String url, SourceStatus status, String notes) {
    }

    public record Note(Long id, String title, String body, Long sourceId, List<String> tags, LocalDateTime createdAt) {
    }

    public record Flashcard(Long id, String front, String back, Long noteId, LocalDate dueOn, int intervalDays,
                            BigDecimal ease, int repetitions, int lapses, LocalDate lastReviewedOn) {
    }

    public record Reminder(String month, String kind, LocalDateTime sentAt) {
    }

    /** @param field MONTHLY_AMOUNT (cents), TICKER or BUY_DAY */
    public record SettingsChange(String field, String oldValue, String newValue, LocalDateTime changedAt) {
    }
}
