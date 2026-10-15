package trademind.tm_server.archive;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import trademind.tm_server.trade.dto.TradeResponse;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

/**
 * Flat, spreadsheet-friendly copy of the trade history: the fields a statement or tax return needs,
 * not the journal analysis. Text cells are guarded so a note starting with a formula character is
 * not executed when the file is opened in a spreadsheet.
 */
public final class TradeArchiveCsv {

    public static final String[] HEADER = {
            "id", "external_id", "symbol", "direction", "entry_time", "exit_time", "entry_price", "exit_price",
            "lot_size", "pnl", "strategy", "source", "notes"};

    private TradeArchiveCsv() {
    }

    public static String export(List<TradeResponse> trades) {
        StringWriter out = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT.builder().setHeader(HEADER).build())) {
            for (TradeResponse t : trades) {
                printer.printRecord(
                        t.getId(),
                        safeCell(t.getExternalId()),
                        safeCell(t.getAsset() == null ? null : t.getAsset().getSymbol()),
                        t.getDirection(),
                        t.getEntryDateTime(),
                        t.getExitDateTime(),
                        t.getEntryPrice(),
                        t.getExitPrice(),
                        t.getLotSize(),
                        t.getPnl(),
                        safeCell(t.getStrategy() == null ? null : t.getStrategy().getName()),
                        t.getSource(),
                        safeCell(t.getNotes()));
            }
        } catch (IOException e) {
            throw new IllegalStateException(e); // StringWriter never throws
        }
        return out.toString();
    }

    static String safeCell(String value) {
        if (value == null || value.isEmpty()) return "";
        char first = value.charAt(0);
        return (first == '=' || first == '+' || first == '-' || first == '@') ? "'" + value : value;
    }
}
