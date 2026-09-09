package trademind.tm_server.staticData;

import org.junit.jupiter.api.Test;
import trademind.tm_server.staticData.dataTypes.BaseStaticDataType;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

// Regression coverage for the exact class of bug Phase 6 found by hand:
// StaticData.SCORE_LIST was silently List.of() (empty -- the discipline
// slider would have had nothing to show), and TRADE_DIRECTIONS,
// PROBABILITIES, VOLATILITIES and TAKE_PROFIT_PLACEMENT_REASONS each had
// every entry sharing the same `order` value (so the UI's sort order for
// those chip lists was arbitrary/unstable, not the intended sequence).
// Nothing caught either class of bug at the time -- they were found by
// reading the file by hand while wiring up something else.
//
// Reflection-based on purpose: it walks every static List field declared on
// StaticData, so a newly added static-data list is covered automatically
// from the moment it's added, without anyone having to remember to write a
// matching test for it.
class StaticDataTest {

    @Test
    void everyStaticDataListIsNonEmptyWithUniqueOrders() throws IllegalAccessException {
        List<String> violations = new ArrayList<>();
        int listsChecked = 0;

        for (Field field : StaticData.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || !List.class.isAssignableFrom(field.getType())) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(null);
            if (!(value instanceof List<?> list)) {
                continue;
            }
            listsChecked++;

            if (list.isEmpty()) {
                violations.add(field.getName() + " is empty");
                continue;
            }

            Set<Integer> seenOrders = new HashSet<>();
            for (Object entry : list) {
                if (!(entry instanceof BaseStaticDataType typed)) {
                    violations.add(field.getName() + " has an entry that isn't a BaseStaticDataType: " + entry);
                    continue;
                }
                if (!seenOrders.add(typed.getOrder())) {
                    violations.add(field.getName() + " has a duplicate order value: " + typed.getOrder());
                }
            }
        }

        assertTrue(listsChecked > 0, "Expected to find at least one static-data list field via reflection");
        assertTrue(violations.isEmpty(), "Static data integrity violations:\n" + String.join("\n", violations));
    }
}
