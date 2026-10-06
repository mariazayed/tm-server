package trademind.tm_server.invest.dto;

import java.util.List;

/**
 * Outcome of a CSV import. Import is all-or-nothing: when {@code errors} is not empty nothing was
 * saved. Rows identical to an existing contribution are skipped, so re-importing is safe.
 */
public record ImportResult(int imported, int skipped, List<RowError> errors) {

    /** @param line 1-based line number in the uploaded CSV (the header is line 1) */
    public record RowError(long line, String message) {
    }
}
