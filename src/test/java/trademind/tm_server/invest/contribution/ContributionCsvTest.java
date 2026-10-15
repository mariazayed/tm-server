package trademind.tm_server.invest.contribution;

import org.junit.jupiter.api.Test;
import trademind.tm_server.invest.Contribution;
import trademind.tm_server.invest.dto.ContributionRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContributionCsvTest {

    private static trademind.tm_server.invest.Holding fund(String ticker) {
        trademind.tm_server.invest.Holding h = new trademind.tm_server.invest.Holding();
        h.setTicker(ticker);
        return h;
    }

    @Test
    void exportThenParseRoundTrips() {
        Contribution c = new Contribution();
        c.setHolding(fund("VT"));
        c.setContributedOn(LocalDate.parse("2026-10-01"));
        c.setAmountCents(30000);
        c.setShares(new BigDecimal("2.55489022"));
        c.setPrice(new BigDecimal("117.4250"));
        c.setFeeCents(150);
        c.setNotes("Bought, then \"breathed\"");

        String csv = ContributionCsv.export(List.of(c));
        assertTrue(csv.startsWith("date,amount,shares,price,fee,notes"));

        ContributionCsv.ParseResult parsed = ContributionCsv.parse(csv);
        assertTrue(parsed.errors().isEmpty(), parsed.errors().toString());
        ContributionRequest r = parsed.rows().get(0).request();
        assertEquals(30000L, r.amountCents());
        assertEquals(150L, r.feeCents());
        assertEquals("Bought, then \"breathed\"", r.notes());
        assertEquals(0, new BigDecimal("2.55489022").compareTo(r.shares()));
    }

    @Test
    void acceptsAliasesCurrencySymbolsAndThousandsSeparators() {
        String csv = "Trade Date,Total,Quantity,Price,Commission\n2026-09-01,\"$1,200.00\",10.2,117.64,0\n";
        ContributionCsv.ParseResult parsed = ContributionCsv.parse(csv);
        assertTrue(parsed.errors().isEmpty(), parsed.errors().toString());
        assertEquals(120000L, parsed.rows().get(0).request().amountCents());
    }

    @Test
    void feeAndNotesAreOptional() {
        ContributionCsv.ParseResult parsed =
                ContributionCsv.parse("date,amount,shares,price\n2026-09-01,300,2.55,117.64\n");
        assertTrue(parsed.errors().isEmpty());
        assertEquals(0L, parsed.rows().get(0).request().feeCents());
        assertEquals(null, parsed.rows().get(0).request().notes());
    }

    @Test
    void reportsEveryBadRowWithItsLineNumber() {
        String csv = """
                date,amount,shares,price
                2026-09-01,300,2.55,117.64
                not-a-date,300,2.55,117.64
                2026-09-02,abc,2.55,117.64
                2026-09-03,300.123,2.55,117.64
                2026-09-04,-5,2.55,117.64
                """;
        ContributionCsv.ParseResult parsed = ContributionCsv.parse(csv);
        assertEquals(1, parsed.rows().size());
        assertEquals(List.of(3L, 4L, 5L, 6L), parsed.errors().stream().map(e -> e.line()).toList());
    }

    @Test
    void missingRequiredColumnIsReported() {
        ContributionCsv.ParseResult parsed = ContributionCsv.parse("date,amount,shares\n2026-09-01,300,2.55\n");
        assertEquals(1, parsed.errors().size());
        assertTrue(parsed.errors().get(0).message().contains("price"));
    }

    @Test
    void exportNeutralisesFormulaStyleNotes() {
        Contribution c = new Contribution();
        c.setHolding(fund("VT"));
        c.setContributedOn(LocalDate.parse("2026-10-01"));
        c.setAmountCents(100);
        c.setShares(BigDecimal.ONE);
        c.setPrice(BigDecimal.ONE);
        c.setNotes("=HYPERLINK(\"http://evil\")");
        assertTrue(ContributionCsv.export(List.of(c)).contains("'=HYPERLINK"));
    }
}
