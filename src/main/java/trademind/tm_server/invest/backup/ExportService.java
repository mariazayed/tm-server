package trademind.tm_server.invest.backup;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.InvestTag;
import trademind.tm_server.invest.PriceQuote;
import trademind.tm_server.invest.repository.ContributionRepository;
import trademind.tm_server.invest.repository.FlashcardRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.InvestTagRepository;
import trademind.tm_server.invest.repository.JournalEntryRepository;
import trademind.tm_server.invest.repository.LearningNoteRepository;
import trademind.tm_server.invest.repository.LearningSourceRepository;
import trademind.tm_server.invest.repository.PriceQuoteRepository;
import trademind.tm_server.invest.repository.ReminderLogRepository;
import trademind.tm_server.invest.repository.SettingsChangeRepository;
import trademind.tm_server.invest.repository.RuleRepository;

import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

/** Reads every invest table into one {@link ExportData} snapshot. */
@Service
@Transactional(readOnly = true)
public class ExportService {

    private final InvestSettingsRepository settings;
    private final ContributionRepository contributions;
    private final PriceQuoteRepository prices;
    private final InvestTagRepository tags;
    private final JournalEntryRepository journal;
    private final RuleRepository rules;
    private final LearningSourceRepository sources;
    private final LearningNoteRepository notes;
    private final FlashcardRepository flashcards;
    private final ReminderLogRepository reminders;
    private final SettingsChangeRepository settingsChanges;
    private final Clock clock;

    public ExportService(InvestSettingsRepository settings, ContributionRepository contributions,
                         PriceQuoteRepository prices, InvestTagRepository tags, JournalEntryRepository journal,
                         RuleRepository rules, LearningSourceRepository sources, LearningNoteRepository notes,
                         FlashcardRepository flashcards, ReminderLogRepository reminders,
                         SettingsChangeRepository settingsChanges, Clock clock) {
        this.settings = settings;
        this.contributions = contributions;
        this.prices = prices;
        this.tags = tags;
        this.journal = journal;
        this.rules = rules;
        this.sources = sources;
        this.notes = notes;
        this.flashcards = flashcards;
        this.reminders = reminders;
        this.settingsChanges = settingsChanges;
        this.clock = clock;
    }

    public ExportData export() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        var liveRules = rules.findByDeletedAtIsNullOrderBySortOrderAsc();
        return new ExportData(
                ExportData.SCHEMA_VERSION,
                clock.instant(),
                new ExportData.Settings(s.getMonthlyAmountCents(), s.getCurrency(), s.getBuyDay(), s.getTicker(),
                        s.getStartDate(), s.getTimezone(), s.isRemindersEnabled(), s.getReminderEmail(),
                        s.getReminderFollowupDays(), s.getRuleWaitHours()),
                contributions.findByDeletedAtIsNullOrderByContributedOnDesc().stream()
                        .sorted(Comparator.comparing(trademind.tm_server.invest.Contribution::getContributedOn)
                                .thenComparing(trademind.tm_server.invest.Contribution::getId))
                        .map(c -> new ExportData.Contribution(c.getId(), c.getContributedOn(), c.getAmountCents(),
                                c.getShares(), c.getPrice(), c.getFeeCents(), c.getNotes(), c.getCreatedAt()))
                        .toList(),
                prices.findAll().stream()
                        .sorted(Comparator.comparing(PriceQuote::getTicker).thenComparing(PriceQuote::getPriceDate))
                        .map(p -> new ExportData.Price(p.getTicker(), p.getPriceDate(), p.getClose())).toList(),
                tags.findByDeletedAtIsNullOrderByNameAsc().stream().map(t -> new ExportData.Tag(t.getName())).toList(),
                journal.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                        .map(e -> new ExportData.JournalEntry(e.getId(), e.getEntryType(), e.getMood(), e.getBody(),
                                tagNames(e.getTags()),
                                e.getContribution() == null || e.getContribution().getDeletedAt() != null
                                        ? null : e.getContribution().getId(),
                                e.getCreatedAt())).toList(),
                IntStream.range(0, liveRules.size())
                        .mapToObj(i -> new ExportData.Rule(liveRules.get(i).getId(), liveRules.get(i).getText(), i + 1))
                        .toList(),
                sources.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                        .map(x -> new ExportData.Source(x.getId(), x.getTitle(), x.getSourceType(), x.getUrl(),
                                x.getStatus(), x.getNotes())).toList(),
                notes.findByDeletedAtIsNullOrderByCreatedAtDesc().stream()
                        .map(n -> new ExportData.Note(n.getId(), n.getTitle(), n.getBody(),
                                n.getSource() != null && n.getSource().getDeletedAt() == null ? n.getSource().getId() : null,
                                tagNames(n.getTags()), n.getCreatedAt())).toList(),
                flashcards.findByDeletedAtIsNullOrderByDueOnAscIdAsc().stream()
                        .map(c -> new ExportData.Flashcard(c.getId(), c.getFront(), c.getBack(),
                                c.getNote() != null && c.getNote().getDeletedAt() == null ? c.getNote().getId() : null,
                                c.getDueOn(), c.getIntervalDays(), c.getEase(), c.getRepetitions(), c.getLapses(),
                                c.getLastReviewedOn())).toList(),
                reminders.findAll().stream()
                        .sorted(Comparator.comparing(trademind.tm_server.invest.reminder.ReminderLog::getSentAt))
                        .map(r -> new ExportData.Reminder(r.getMonth(), r.getKind().name(), r.getSentAt())).toList(),
                settingsChanges.findByDeletedAtIsNullOrderByCreatedAtAscIdAsc().stream()
                        .map(c -> new ExportData.SettingsChange(c.getFieldName(), c.getOldValue(), c.getNewValue(),
                                c.getCreatedAt())).toList());
    }

    private static List<String> tagNames(java.util.Set<InvestTag> tags) {
        return tags.stream().filter(t -> t.getDeletedAt() == null).map(InvestTag::getName).sorted().toList();
    }
}
