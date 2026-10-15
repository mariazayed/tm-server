package trademind.tm_server.features;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard against a trading endpoint being added without the gate. Every controller's base path must be
 * either in {@link TradingGateFilter#TRADING_PREFIXES} (hidden while trading is off) or deliberately
 * listed below as always-available. A new controller fails this test until someone decides which.
 */
class TradingGateCoverageTest {

    /** Paths that stay open with trading off. Add a path here only if it is not trading-only. */
    private static final List<String> ALWAYS_AVAILABLE = List.of(
            "/api/v1/invest", "/api/v1/features", "/api/v1/archive", "/api/v1/staticData",
            "/api/v1/goal", "/api/v1/decision", "/api/v1/planner",
            "/api/v1/expense", "/api/v1/expenseReason", "/api/v1/questionAnswers",
            "/api/v1/questionCategories", "/api/v1/reminderNotes");

    private static List<String> controllerBasePaths() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> paths = new ArrayList<>();
        for (BeanDefinition bd : scanner.findCandidateComponents("trademind.tm_server")) {
            Class<?> type = Class.forName(bd.getBeanClassName());
            RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(type, RequestMapping.class);
            String[] values = mapping == null ? new String[0] : mapping.path();
            assertTrue(values.length > 0, type.getSimpleName() + " has no @RequestMapping base path");
            paths.add(values[0] + "|" + type.getSimpleName());
        }
        return paths;
    }

    private static boolean under(String path, String prefix) {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }

    @Test
    void everyControllerIsEitherGatedOrDeliberatelyOpen() throws Exception {
        Set<String> unclassified = new TreeSet<>();
        int seen = 0;
        for (String entry : controllerBasePaths()) {
            seen++;
            String path = entry.substring(0, entry.indexOf('|'));
            boolean gated = TradingGateFilter.isTradingPath(path);
            boolean open = ALWAYS_AVAILABLE.stream().anyMatch(p -> under(path, p));
            if (!gated && !open) unclassified.add(entry);
        }
        assertTrue(seen >= 25, "controller scan looks broken, only found " + seen);
        assertEquals(Set.of(), unclassified,
                "These controllers are neither gated nor listed as always-available. Decide which: add the "
                        + "path to TradingGateFilter.TRADING_PREFIXES (trading-only) or to ALWAYS_AVAILABLE.");
    }

    @Test
    void noPathIsBothGatedAndAlwaysAvailable() {
        for (String prefix : TradingGateFilter.TRADING_PREFIXES) {
            for (String open : ALWAYS_AVAILABLE) {
                assertTrue(!under(prefix, open) && !under(open, prefix), prefix + " overlaps " + open);
            }
        }
    }

    @Test
    void everyGatedPrefixHasAController() throws Exception {
        List<String> paths = controllerBasePaths().stream().map(e -> e.substring(0, e.indexOf('|'))).toList();
        for (String prefix : TradingGateFilter.TRADING_PREFIXES) {
            assertTrue(paths.stream().anyMatch(p -> under(p, prefix)), "no controller serves gated prefix " + prefix);
        }
    }
}
