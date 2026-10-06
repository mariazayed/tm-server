package trademind.tm_server.invest.journal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import trademind.tm_server.enums.JournalEntryType;

import java.time.LocalDateTime;
import java.util.List;

public final class JournalDtos {

    private JournalDtos() {
    }

    /**
     * @param tags           names; created on first use, matched case-insensitively, stored lower-case
     * @param contributionId optional link to the buy this entry is about
     */
    public record EntryRequest(
            @NotNull JournalEntryType entryType,
            @Min(1) @Max(5) Short mood,
            @NotBlank @Size(max = JournalService.MAX_BODY) String body,
            @Size(max = JournalService.MAX_TAGS) List<@NotBlank @Size(max = 64) String> tags,
            Long contributionId) {
    }

    public record EntryResponse(
            Long id,
            JournalEntryType entryType,
            Short mood,
            String body,
            List<String> tags,
            Long contributionId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
    }

    public record PageResponse(List<EntryResponse> items, int page, int size, long total, boolean hasMore) {
    }

    public record TagResponse(String name, long count) {
    }

    /** A pre-filled monthly check-in; {@code contributionId} is this month's latest buy, if any. */
    public record CheckinTemplate(String month, String body, Long contributionId) {
    }
}
