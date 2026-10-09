package trademind.tm_server.invest.ai;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Keeps only the ids the model was actually shown. A model can invent an entry, rule or note id; an
 * invented id must never become a link, a citation or a suggestion about something that is not there.
 */
public final class KnownIds {

    private KnownIds() {
    }

    /** The proposed ids that are in {@code allowed}, in the model's order, without repeats; null entries are dropped. */
    public static List<Long> keep(Collection<Long> proposed, Set<Long> allowed) {
        if (proposed == null) return List.of();
        Set<Long> kept = new LinkedHashSet<>();
        for (Long id : proposed) {
            if (id != null && allowed.contains(id)) kept.add(id);
        }
        return List.copyOf(kept);
    }

    /** True only for an id that was shown. */
    public static boolean isKnown(Long id, Set<Long> allowed) {
        return id != null && allowed.contains(id);
    }
}
