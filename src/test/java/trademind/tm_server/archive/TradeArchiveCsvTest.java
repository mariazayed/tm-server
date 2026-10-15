package trademind.tm_server.archive;

import org.junit.jupiter.api.Test;
import trademind.tm_server.asset.dto.AssetResponse;
import trademind.tm_server.trade.dto.TradeResponse;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeArchiveCsvTest {

    private static TradeResponse trade(Long id, String symbol, double pnl, String notes) {
        TradeResponse t = new TradeResponse();
        t.setId(id);
        AssetResponse a = new AssetResponse();
        a.setSymbol(symbol);
        t.setAsset(a);
        t.setEntryDateTime(LocalDateTime.parse("2026-01-02T09:30:00"));
        t.setExitDateTime(LocalDateTime.parse("2026-01-02T11:00:00"));
        t.setEntryPrice(1.1);
        t.setExitPrice(1.2);
        t.setLotSize(0.5);
        t.setPnl(pnl);
        t.setNotes(notes);
        return t;
    }

    @Test
    void headerAndOneRow() {
        String csv = TradeArchiveCsv.export(List.of(trade(7L, "EURUSD", -12.5, "ok")));
        String[] lines = csv.split("\r?\n");
        assertEquals(String.join(",", TradeArchiveCsv.HEADER), lines[0]);
        assertTrue(lines[1].startsWith("7,,EURUSD,,2026-01-02T09:30,2026-01-02T11:00,1.1,1.2,0.5,-12.5,"), lines[1]);
    }

    @Test
    void emptyHistoryIsHeaderOnly() {
        assertEquals(1, TradeArchiveCsv.export(List.of()).split("\r?\n").length);
    }

    @Test
    void formulaLookingTextIsDefused_butNumbersAreNot() {
        String csv = TradeArchiveCsv.export(List.of(trade(1L, "=cmd", -3.0, "@SUM(A1)")));
        assertTrue(csv.contains("'=cmd"), csv);
        assertTrue(csv.contains("'@SUM(A1)"), csv);
        assertTrue(csv.contains(",-3.0,"), "negative pnl must stay numeric: " + csv);
    }

    @Test
    void commasAndQuotesInNotesAreQuoted() {
        String csv = TradeArchiveCsv.export(List.of(trade(1L, "X", 0, "a, \"b\"")));
        assertTrue(csv.contains("\"a, \"\"b\"\"\""), csv);
    }
}
