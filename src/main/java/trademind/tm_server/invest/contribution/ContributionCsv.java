package trademind.tm_server.invest.contribution;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import trademind.tm_server.invest.Contribution;
import trademind.tm_server.invest.dto.ContributionRequest;
import trademind.tm_server.invest.dto.ImportResult.RowError;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * CSV import/export. Columns: date, amount, shares, price, fee, notes. Amount and fee are in
 * currency units (300.00), not cents. Headers are matched case-insensitively with a few aliases.
 */
public final class ContributionCsv {

    public static final int MAX_ROWS = 5000;

    public static final String[] HEADER = {"date", "amount", "shares", "price", "fee", "notes"};

    private static final Map<String, String> ALIASES = Map.ofEntries(
            Map.entry("date", "date"), Map.entry("trade date", "date"),
            Map.entry("amount", "amount"), Map.entry("total", "amount"),
            Map.entry("shares", "shares"), Map.entry("quantity", "shares"),
            Map.entry("price", "price"),
            Map.entry("fee", "fee"), Map.entry("fees", "fee"), Map.entry("commission", "fee"),
            Map.entry("notes", "notes"), Map.entry("note", "notes"));

    public record ParsedRow(long line, ContributionRequest request) {
    }

    public record ParseResult(List<ParsedRow> rows, List<RowError> errors) {
    }

    private ContributionCsv() {
    }

    public static String export(List<Contribution> contributions) {
        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(HEADER).build())) {
            for (Contribution c : contributions) {
                printer.printRecord(c.getContributedOn(), cents(c.getAmountCents()),
                        c.getShares().toPlainString(), c.getPrice().toPlainString(),
                        cents(c.getFeeCents()), safeCell(c.getNotes()));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e); // StringWriter never throws
        }
        return out.toString();
    }

    public static ParseResult parse(String csv) {
        List<ParsedRow> rows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).setIgnoreEmptyLines(true).setTrim(true).build();
        try (CSVParser parser = format.parse(new StringReader(csv))) {
            Map<String, String> columns = new java.util.HashMap<>(); // canonical -> actual header
            for (String h : parser.getHeaderNames()) {
                String canonical = ALIASES.get(h.trim().toLowerCase(Locale.ROOT));
                if (canonical != null) columns.putIfAbsent(canonical, h);
            }
            for (String required : List.of("date", "amount", "shares", "price")) {
                if (!columns.containsKey(required)) {
                    errors.add(new RowError(1, "Missing required column: " + required));
                }
            }
            if (!errors.isEmpty()) return new ParseResult(rows, errors);

            for (CSVRecord rec : parser) {
                if (rows.size() + errors.size() >= MAX_ROWS) {
                    errors.add(new RowError(rec.getRecordNumber() + 1, "Too many rows (max " + MAX_ROWS + ")"));
                    break;
                }
                long line = rec.getRecordNumber() + 1; // header is line 1
                try {
                    rows.add(new ParsedRow(line, toRequest(rec, columns)));
                } catch (IllegalArgumentException e) {
                    errors.add(new RowError(line, e.getMessage()));
                }
            }
        } catch (IOException | IllegalStateException e) {
            errors.add(new RowError(1, "Could not read CSV: " + e.getMessage()));
        }
        return new ParseResult(rows, errors);
    }

    private static ContributionRequest toRequest(CSVRecord rec, Map<String, String> columns) {
        LocalDate date;
        try {
            date = LocalDate.parse(cell(rec, columns, "date"));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date must be YYYY-MM-DD");
        }
        long amount = toCents(decimal(rec, columns, "amount"), "amount");
        BigDecimal shares = decimal(rec, columns, "shares");
        BigDecimal price = decimal(rec, columns, "price");
        String feeText = columns.containsKey("fee") ? cell(rec, columns, "fee") : "";
        long fee = feeText.isEmpty() ? 0 : toCents(parseDecimal(feeText, "fee"), "fee");
        String notes = columns.containsKey("notes") ? cell(rec, columns, "notes") : null;
        if (amount <= 0 || shares.signum() <= 0 || price.signum() <= 0 || fee < 0) {
            throw new IllegalArgumentException("Amount, shares and price must be positive; fee cannot be negative");
        }
        return new ContributionRequest(date, amount, shares, price, fee,
                notes == null || notes.isEmpty() ? null : notes);
    }

    private static String cell(CSVRecord rec, Map<String, String> columns, String name) {
        String header = columns.get(name);
        return rec.isSet(header) ? rec.get(header) : "";
    }

    private static BigDecimal decimal(CSVRecord rec, Map<String, String> columns, String name) {
        return parseDecimal(cell(rec, columns, name), name);
    }

    private static BigDecimal parseDecimal(String text, String name) {
        try {
            return new BigDecimal(text.replace("$", "").replace(",", ""));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number for " + name + ": '" + text + "'");
        }
    }

    private static long toCents(BigDecimal value, String name) {
        try {
            return value.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(name + " must have at most 2 decimal places");
        }
    }

    private static String cents(long cents) {
        return BigDecimal.valueOf(cents).movePointLeft(2).toPlainString();
    }

    /** Stops spreadsheet apps from executing a note that starts with a formula character. */
    private static String safeCell(String value) {
        if (value == null || value.isEmpty()) return "";
        char first = value.charAt(0);
        return (first == '=' || first == '+' || first == '-' || first == '@') ? "'" + value : value;
    }
}
