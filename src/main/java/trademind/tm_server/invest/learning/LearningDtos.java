package trademind.tm_server.invest.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import trademind.tm_server.enums.SourceStatus;
import trademind.tm_server.enums.SourceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class LearningDtos {

    public static final int MAX_NOTE_BODY = 20_000;
    public static final int MAX_CARD_SIDE = 2_000;

    private LearningDtos() {
    }

    /** @param status defaults to TO_DO; @param url must be http or https when present */
    public record SourceRequest(
            @NotBlank @Size(max = 255) String title,
            @NotNull SourceType sourceType,
            @Size(max = 2048) String url,
            SourceStatus status,
            @Size(max = 2000) String notes) {
    }

    public record SourceResponse(Long id, String title, SourceType sourceType, String url, SourceStatus status,
                                 String notes, LocalDateTime createdAt) {
    }

    public record NoteRequest(
            @NotBlank @Size(max = 255) String title,
            @NotBlank @Size(max = MAX_NOTE_BODY) String body,
            Long sourceId,
            @Size(max = 10) List<@NotBlank @Size(max = 64) String> tags) {
    }

    public record NoteResponse(Long id, String title, String body, Long sourceId, String sourceTitle,
                               List<String> tags, LocalDateTime createdAt, LocalDateTime updatedAt) {
    }

    public record NotePage(List<NoteResponse> items, int page, int size, long total, boolean hasMore) {
    }

    public record CardRequest(
            @NotBlank @Size(max = MAX_CARD_SIDE) String front,
            @NotBlank @Size(max = MAX_CARD_SIDE) String back,
            Long noteId) {
    }

    public record CardResponse(Long id, String front, String back, Long noteId, LocalDate dueOn, int intervalDays,
                               BigDecimal ease, int repetitions, int lapses, LocalDate lastReviewedOn) {
    }

    public record ReviewRequest(@NotNull Sm2Scheduler.Grade grade) {
    }

    /** @param newCards cards never reviewed; @param due cards due today or overdue */
    public record CardStats(long total, long due, long newCards, long reviewedToday) {
    }
}
