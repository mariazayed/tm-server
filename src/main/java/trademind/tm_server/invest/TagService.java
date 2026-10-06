package trademind.tm_server.invest;

import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.repository.InvestTagRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Shared tag rules for journal entries and learning notes. */
@Service
public class TagService {

    public static final int MAX_TAGS = 10;

    private final InvestTagRepository tags;

    public TagService(InvestTagRepository tags) {
        this.tags = tags;
    }

    /** Trim, drop a leading '#', collapse spaces, lower-case, dedupe, keep order. */
    public static List<String> normalize(List<String> raw) {
        if (raw == null) return List.of();
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String t : raw) {
            String n = t.strip().replaceFirst("^#+", "").strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
            if (n.isEmpty()) continue;
            out.add(n);
        }
        if (out.size() > MAX_TAGS) {
            throw new BadRequestException("At most " + MAX_TAGS + " tags per entry");
        }
        return new ArrayList<>(out);
    }

    /** Finds each tag case-insensitively or creates it on first use. */
    public Set<InvestTag> resolve(List<String> raw) {
        Set<InvestTag> resolved = new LinkedHashSet<>();
        for (String name : normalize(raw)) {
            resolved.add(tags.findByNameIgnoreCaseAndDeletedAtIsNull(name)
                    .orElseGet(() -> tags.save(new InvestTag(name))));
        }
        return resolved;
    }
}
