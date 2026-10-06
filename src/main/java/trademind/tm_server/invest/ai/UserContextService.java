package trademind.tm_server.invest.ai;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.contribution.ContributionService;
import trademind.tm_server.invest.dto.ContributionResponse;
import trademind.tm_server.invest.journal.JournalService;
import trademind.tm_server.invest.learning.FlashcardService;
import trademind.tm_server.invest.learning.LearningService;
import trademind.tm_server.invest.performance.PerformanceResponse;
import trademind.tm_server.invest.performance.PerformanceService;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.rules.RulesService;

import java.time.Clock;
import java.util.List;

/**
 * Builds the {@link UserContext}: a bounded, up-to-date snapshot for AI features. The size limits here
 * are the contract: the context never grows without bound as the journal grows.
 */
@Service
@Transactional(readOnly = true)
public class UserContextService {

    public static final int RECENT_BUYS = 6;
    public static final int RECENT_ENTRIES = 5;
    public static final int RECENT_NOTES = 5;
    public static final int MAX_ENTRY_CHARS = 400;
    public static final int MAX_NOTE_CHARS = 240;

    private final InvestSettingsRepository settings;
    private final RulesService rules;
    private final PerformanceService performance;
    private final ContributionService contributions;
    private final JournalService journal;
    private final LearningService learning;
    private final FlashcardService flashcards;
    private final Clock clock;

    public UserContextService(InvestSettingsRepository settings, RulesService rules, PerformanceService performance,
                              ContributionService contributions, JournalService journal, LearningService learning,
                              FlashcardService flashcards, Clock clock) {
        this.settings = settings;
        this.rules = rules;
        this.performance = performance;
        this.contributions = contributions;
        this.journal = journal;
        this.learning = learning;
        this.flashcards = flashcards;
        this.clock = clock;
    }

    /** The owner's context, without free text unless the options allow it. */
    public UserContext build(ContextOptions options) {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        PerformanceResponse p = performance.performance("ALL");
        var status = contributions.status();
        var cards = flashcards.stats();

        List<UserContext.Buy> buys = contributions.list().stream().limit(RECENT_BUYS)
                .map(UserContextService::toBuy).toList();

        List<UserContext.Entry> entries = options.includeJournal()
                ? journal.list(new JournalService.Filter(null, null, null, null, null), 0, RECENT_ENTRIES).items().stream()
                        .map(e -> new UserContext.Entry("journal#" + e.id(), e.createdAt().toLocalDate(),
                                e.entryType().name(), e.mood(), e.tags(), clip(e.body(), MAX_ENTRY_CHARS)))
                        .toList()
                : List.of();

        List<UserContext.Note> notes = options.includeNotes()
                ? learning.listNotes(new LearningService.NoteFilter(null, null, null), 0, RECENT_NOTES).items().stream()
                        .map(n -> new UserContext.Note("note#" + n.id(), n.createdAt().toLocalDate(), n.title(),
                                n.sourceTitle(), n.tags(), clip(n.body(), MAX_NOTE_CHARS)))
                        .toList()
                : List.of();

        var current = status.months().isEmpty() ? null : status.months().get(status.months().size() - 1);
        return new UserContext(
                clock.instant(),
                new UserContext.Plan(s.getMonthlyAmountCents(), s.getCurrency(), s.getBuyDay(), s.getTicker(), s.getStartDate()),
                rules.list().stream().map(RulesService.RuleResponse::text).toList(),
                new UserContext.Portfolio(p.investedCents(), p.valueCents(), p.gainCents(), p.gainPercent(), p.shares(),
                        p.averageCost(), p.priceAsOf(), p.priceFromCache(), p.stale()),
                new UserContext.Consistency(current == null ? "NONE" : current.state().name(),
                        status.currentStreak(), status.longestStreak(), status.missedCount(), status.currentDueDate()),
                buys, entries, notes,
                new UserContext.Cards(cards.total(), cards.due(), cards.newCards()),
                false);
    }

    private static UserContext.Buy toBuy(ContributionResponse c) {
        return new UserContext.Buy(c.contributedOn(), c.amountCents(), c.shares(), c.price());
    }

    /** Collapses whitespace and cuts to {@code max} characters, marking the cut. */
    static String clip(String text, int max) {
        String flat = text.strip().replaceAll("\\s+", " ");
        if (flat.length() <= max) return flat;
        int cut = max;
        // do not split a surrogate pair
        if (Character.isHighSurrogate(flat.charAt(cut - 1))) cut--;
        return flat.substring(0, cut).stripTrailing() + "…";
    }
}
