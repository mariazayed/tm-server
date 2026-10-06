package trademind.tm_server.invest.learning;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.Flashcard;
import trademind.tm_server.invest.InvestSettings;
import trademind.tm_server.invest.learning.LearningDtos.CardRequest;
import trademind.tm_server.invest.learning.LearningDtos.CardResponse;
import trademind.tm_server.invest.learning.LearningDtos.CardStats;
import trademind.tm_server.invest.repository.FlashcardRepository;
import trademind.tm_server.invest.repository.InvestSettingsRepository;
import trademind.tm_server.invest.repository.LearningNoteRepository;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Transactional
public class FlashcardService {

    public static final int MAX_QUEUE = 100;

    private final FlashcardRepository cards;
    private final LearningNoteRepository notes;
    private final InvestSettingsRepository settings;
    private final Clock clock;

    public FlashcardService(FlashcardRepository cards, LearningNoteRepository notes,
                            InvestSettingsRepository settings, Clock clock) {
        this.cards = cards;
        this.notes = notes;
        this.settings = settings;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CardResponse> list(Long noteId) {
        var found = noteId == null
                ? cards.findByDeletedAtIsNullOrderByDueOnAscIdAsc()
                : cards.findByDeletedAtIsNullAndNote_IdOrderByIdAsc(noteId);
        return found.stream().map(FlashcardService::toResponse).toList();
    }

    /** Today's queue: due or overdue cards, most overdue first. */
    @Transactional(readOnly = true)
    public List<CardResponse> due(int limit) {
        if (limit < 1 || limit > MAX_QUEUE) {
            throw new BadRequestException("limit must be between 1 and " + MAX_QUEUE);
        }
        return cards.findByDeletedAtIsNullAndDueOnLessThanEqualOrderByDueOnAscIdAsc(today(), PageRequest.of(0, limit))
                .stream().map(FlashcardService::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CardStats stats() {
        LocalDate today = today();
        return new CardStats(cards.countByDeletedAtIsNull(),
                cards.countByDeletedAtIsNullAndDueOnLessThanEqual(today),
                cards.countByDeletedAtIsNullAndLastReviewedOnIsNull(),
                cards.countByDeletedAtIsNullAndLastReviewedOn(today));
    }

    public CardResponse create(CardRequest r) {
        Flashcard c = new Flashcard();
        c.setDueOn(today()); // a new card is due straight away
        apply(c, r);
        return toResponse(cards.save(c));
    }

    /** Editing text never touches the schedule. */
    public CardResponse update(Long id, CardRequest r) {
        Flashcard c = find(id);
        apply(c, r);
        return toResponse(cards.save(c));
    }

    public void delete(Long id) {
        Flashcard c = find(id);
        c.setDeletedAt(LocalDateTime.now(clock));
        cards.save(c);
    }

    public CardResponse review(Long id, Sm2Scheduler.Grade grade) {
        Flashcard c = find(id);
        LocalDate today = today();
        Sm2Scheduler.Result next = Sm2Scheduler.review(
                new Sm2Scheduler.State(c.getRepetitions(), c.getIntervalDays(), c.getEase(), c.getLapses()), grade, today);
        c.setRepetitions(next.state().repetitions());
        c.setIntervalDays(next.state().intervalDays());
        c.setEase(next.state().ease());
        c.setLapses(next.state().lapses());
        c.setDueOn(next.dueOn());
        c.setLastReviewedOn(today);
        return toResponse(cards.save(c));
    }

    private void apply(Flashcard c, CardRequest r) {
        if (r.front().isBlank() || r.back().isBlank()) {
            throw new BadRequestException("Both sides of the card need text");
        }
        c.setFront(r.front().strip());
        c.setBack(r.back().strip());
        c.setNote(r.noteId() == null ? null : notes.findByIdAndDeletedAtIsNull(r.noteId())
                .orElseThrow(() -> new BadRequestException("Learning note " + r.noteId() + " not found")));
    }

    private Flashcard find(Long id) {
        return cards.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Flashcard " + id + " not found"));
    }

    private LocalDate today() {
        InvestSettings s = settings.findById(InvestSettings.SINGLETON_ID).orElseThrow();
        return LocalDate.now(clock.withZone(ZoneId.of(s.getTimezone())));
    }

    private static CardResponse toResponse(Flashcard c) {
        boolean liveNote = c.getNote() != null && c.getNote().getDeletedAt() == null;
        return new CardResponse(c.getId(), c.getFront(), c.getBack(), liveNote ? c.getNote().getId() : null,
                c.getDueOn(), c.getIntervalDays(), c.getEase(), c.getRepetitions(), c.getLapses(),
                c.getLastReviewedOn());
    }
}
