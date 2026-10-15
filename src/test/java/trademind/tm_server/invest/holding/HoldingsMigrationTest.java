package trademind.tm_server.invest.holding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs V22 to V29, adds data the way an existing install has it, then runs V30 and checks that nothing was
 * lost and every buy now belongs to a fund. Works in its own throwaway PostgreSQL schema, so it does not touch
 * the invest_* tables the other tests use.
 */
@EnabledIfEnvironmentVariable(named = "INVEST_TEST_DB_URL", matches = ".+")
class HoldingsMigrationTest {

    private static final String[] BEFORE = {
            "V22__create_invest_tables.sql", "V23__create_invest_flashcards.sql", "V24__invest_reminders.sql",
            "V25__invest_embeddings.sql", "V26__invest_ai_usage.sql", "V27__invest_fulltext_search.sql",
            "V28__invest_rule_wait_hours.sql", "V29__invest_settings_changes.sql"};

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(System.getenv("INVEST_TEST_DB_URL"),
                System.getenv().getOrDefault("INVEST_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("INVEST_TEST_DB_PASSWORD", ""));
    }

    private static void run(Connection c, String file) {
        ScriptUtils.executeSqlScript(c, new ClassPathResource("db/migration/" + file));
    }

    private static long count(Statement st, String sql) throws SQLException {
        try (ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }

    @Test
    void anExistingInstallKeepsItsBuysAndGainsOneFund() throws Exception {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS mig_v30 CASCADE");
            st.execute("CREATE SCHEMA mig_v30");
            st.execute("SET search_path TO mig_v30, public");
            try {
                for (String f : BEFORE) run(c, f);

                st.execute("UPDATE invest_settings SET ticker = 'XYZ'");
                st.execute("INSERT INTO invest_contributions (contributed_on, amount_cents, shares, price) VALUES "
                        + "('2026-08-01', 30000, 3, 100), ('2026-09-01', 30000, 2.5, 120), ('2026-10-01', 30000, 2, 150)");
                st.execute("UPDATE invest_contributions SET deleted_at = now() WHERE contributed_on = '2026-09-01'");

                run(c, "V30__create_invest_holdings.sql");

                assertEquals(1, count(st, "SELECT count(*) FROM invest_holdings"));
                try (ResultSet rs = st.executeQuery("SELECT ticker, asset_class, target_pct, deleted_at FROM invest_holdings")) {
                    assertTrue(rs.next());
                    assertEquals("XYZ", rs.getString("ticker"));
                    assertEquals("EQUITY", rs.getString("asset_class"));
                    assertEquals(0, new java.math.BigDecimal("100").compareTo(rs.getBigDecimal("target_pct")));
                    assertEquals(null, rs.getTimestamp("deleted_at"));
                }
                // every buy, including the one that was soft-deleted, belongs to that fund
                assertEquals(3, count(st, "SELECT count(*) FROM invest_contributions"));
                assertEquals(3, count(st, "SELECT count(*) FROM invest_contributions c JOIN invest_holdings h ON h.id = c.holding_id "
                        + "WHERE h.ticker = 'XYZ'"));
                // the settings ticker is untouched, so code that still reads it keeps working
                assertEquals(1, count(st, "SELECT count(*) FROM invest_settings WHERE ticker = 'XYZ'"));
                // the amounts were not touched
                assertEquals(90000, count(st, "SELECT sum(amount_cents) FROM invest_contributions"));

                // from now on a buy needs a fund, and a ticker can only be live once
                assertThrows(SQLException.class, () -> st.execute(
                        "INSERT INTO invest_contributions (contributed_on, amount_cents, shares, price) VALUES ('2026-10-02', 1, 1, 1)"));
                assertThrows(SQLException.class, () -> st.execute("INSERT INTO invest_holdings (ticker) VALUES ('xyz')"));
                st.execute("INSERT INTO invest_holdings (ticker, deleted_at) VALUES ('xyz', now())"); // a deleted twin is fine
                st.execute("INSERT INTO invest_holdings (ticker, target_pct) VALUES ('AGG', 25)");
                assertThrows(SQLException.class, () -> st.execute("INSERT INTO invest_holdings (ticker, target_pct) VALUES ('BAD', 101)"));
                assertFalse(count(st, "SELECT count(*) FROM invest_holdings") == 0);
            } finally {
                st.execute("RESET search_path");
                st.execute("DROP SCHEMA IF EXISTS mig_v30 CASCADE");
            }
        }
    }

    @Test
    void aFreshInstallStartsWithTheSeededFund() throws Exception {
        try (Connection c = connect(); Statement st = c.createStatement()) {
            st.execute("DROP SCHEMA IF EXISTS mig_v30b CASCADE");
            st.execute("CREATE SCHEMA mig_v30b");
            st.execute("SET search_path TO mig_v30b, public");
            try {
                for (String f : BEFORE) run(c, f);
                run(c, "V30__create_invest_holdings.sql");
                assertEquals(1, count(st, "SELECT count(*) FROM invest_holdings WHERE ticker = 'VT'"));
                assertEquals(0, count(st, "SELECT count(*) FROM invest_contributions"));
            } finally {
                st.execute("RESET search_path");
                st.execute("DROP SCHEMA IF EXISTS mig_v30b CASCADE");
            }
        }
    }
}
