package trademind.tm_server.invest.ai;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.rulecheck.RuleCheckResponse;
import trademind.tm_server.invest.rulecheck.RuleCheckResponse.ModelInfo;
import trademind.tm_server.invest.rulecheck.RuleCheckService;
import trademind.tm_server.invest.rulecheck.RuleFinding;
import trademind.tm_server.invest.rules.RulesService;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The consistency check with the model's reading added to the exact checks. The exact checks always run;
 * the model part runs only when asked for, and when it cannot (AI off, a limit reached, nothing to read, a
 * failed or unusable reply) the exact findings are still returned with the reason, never an error.
 *
 * <p>Suggestion-only and read-only: it reads rules and journal entries and returns findings. Nothing is
 * stored, and no entry is marked. What the model returns is never shown as it came: each finding must
 * name a rule and entries that were really in the prompt, its quote must really be in a cited entry
 * (otherwise it is dropped and the finding kept without it), HIGH confidence needs a verified quote, and a
 * finding that gives investment advice is discarded.
 */
@Service
public class RuleCheckAiService {

    static final int MAX_FINDINGS = 10;
    static final int MAX_REASON_CHARS = 300;
    private static final int FETCH_LIMIT = 200;

    private final RuleCheckService exact;
    private final AiJsonService json;
    private final AiService ai;
    private final RulesService rules;
    private final JdbcTemplate jdbc;
    private final AiProperties properties;

    public RuleCheckAiService(RuleCheckService exact, AiJsonService json, AiService ai, RulesService rules,
                              JdbcTemplate jdbc, AiProperties properties) {
        this.exact = exact;
        this.json = json;
        this.ai = ai;
        this.rules = rules;
        this.jdbc = jdbc;
        this.properties = properties;
    }

    /** The exact checks plus, if it can run, the model's reading of the same range. */
    public RuleCheckResponse run(LocalDate from, LocalDate to) {
        RuleCheckService.Result det = exact.run(from, to); // also validates the range
        Outcome model = modelPart(from, to);
        List<RuleFinding> all = new ArrayList<>(det.findings());
        all.addAll(model.findings);
        all.sort(Comparator.comparing(RuleFinding::date).reversed()
                .thenComparing(RuleFinding::source).thenComparing(RuleFinding::check));
        return new RuleCheckResponse(det.from(), det.to(), det.waitHours(), List.copyOf(all), det.checks(), model.info);
    }

    /** The exact prompt the model part would send, for the "what will be sent" preview. */
    RuleCheckPromptBuilder.Built buildPrompt(LocalDate from, LocalDate to) {
        RuleCheckService.validateRange(from, to);
        List<RuleCheckPromptBuilder.Rule> ruleList = rules.list().stream()
                .map(r -> new RuleCheckPromptBuilder.Rule(r.id(), r.text())).toList();
        return RuleCheckPromptBuilder.build(from, to, ruleList, entries(from, to), count(from, to),
                properties.limits().maxPromptChars());
    }

    // ---- the model part -----------------------------------------------------------------------

    private record Outcome(ModelInfo info, List<RuleFinding> findings) {
        static Outcome without(String reason) {
            return new Outcome(ModelInfo.notRun(reason), List.of());
        }
    }

    private Outcome modelPart(LocalDate from, LocalDate to) {
        try {
            ai.requireAvailable();
            if (rules.list().isEmpty()) return Outcome.without("You have no rules written yet, so there is nothing to check against.");
            RuleCheckPromptBuilder.Built built = buildPrompt(from, to);
            if (built.entries().isEmpty() && built.skipped() == 0) return Outcome.without("There are no journal entries in this range.");

            int[] proposed = {0};
            AiJsonService.Result<RuleCheckModelReply> result = json.call(AiFeature.RULE_CHECK,
                    built.prompt().system(), built.prompt().user(), RuleCheckModelReply.class,
                    reply -> clean(reply, built, proposed));
            List<RuleFinding> findings = toFindings(result.value(), built);
            int dropped = Math.max(0, proposed[0] - findings.size());
            return new Outcome(new ModelInfo(true, true, null, built.entries().size(), built.skipped(), dropped), findings);
        } catch (AiUnavailableException | AiLimitExceededException e) {
            return Outcome.without(e.getMessage()); // fixed, safe wording
        } catch (AiUpstreamException e) {
            return Outcome.without("The AI service did not answer. The exact checks above are still valid.");
        } catch (AiInvalidReplyException e) {
            return Outcome.without("The AI answered in a form that could not be used. The exact checks above are still valid.");
        } catch (BadRequestException e) {
            return Outcome.without("There is too much text to check at once. Choose a shorter range.");
        }
    }

    /**
     * Checks the model's reply against what it was shown. Findings that cannot be trusted are removed; if the
     * model proposed findings and every one was unusable, a repeat is requested instead of silently reporting
     * "nothing found".
     */
    static RuleCheckModelReply clean(RuleCheckModelReply reply, RuleCheckPromptBuilder.Built built, int[] proposed) {
        List<RuleCheckModelReply.Finding> raw = reply.findings() == null ? List.of() : reply.findings();
        proposed[0] = raw.size();
        List<RuleCheckModelReply.Finding> kept = new ArrayList<>();
        for (RuleCheckModelReply.Finding f : raw) {
            RuleCheckModelReply.Finding checked = cleanOne(f, built);
            if (checked != null) kept.add(checked);
        }
        if (!raw.isEmpty() && kept.isEmpty()) {
            throw new ModelJsonException("every finding named a rule or entry that was not in the lists, or gave advice");
        }
        kept.sort(Comparator.comparing((RuleCheckModelReply.Finding f) -> f.confidence()).reversed());
        return new RuleCheckModelReply(List.copyOf(kept.subList(0, Math.min(kept.size(), MAX_FINDINGS))));
    }

    private static RuleCheckModelReply.Finding cleanOne(RuleCheckModelReply.Finding f, RuleCheckPromptBuilder.Built built) {
        String rule = handle(f.rule());
        if (rule == null || !built.ruleIds().containsKey(rule)) return null;

        Set<String> entryHandles = new LinkedHashSet<>();
        if (f.entries() != null) {
            for (String raw : f.entries()) {
                String h = handle(raw);
                if (h != null && built.entries().containsKey(h)) entryHandles.add(h);
            }
        }
        if (entryHandles.isEmpty()) return null;

        String reason = f.reason() == null ? "" : f.reason().replaceAll("\\s+", " ").strip();
        if (reason.isEmpty() || ChatAnswerCheck.givesAdvice(reason)) return null;
        if (reason.length() > MAX_REASON_CHARS) reason = reason.substring(0, MAX_REASON_CHARS - 1) + "…";

        List<String> cited = entryHandles.stream().map(h -> built.entries().get(h).sent()).toList();
        String quote = f.quote() == null ? null : QuoteCheck.verified(f.quote(), cited).orElse(null);

        RuleCheckModelReply.Confidence confidence = f.confidence() == null ? RuleCheckModelReply.Confidence.LOW : f.confidence();
        if (confidence == RuleCheckModelReply.Confidence.HIGH && quote == null) {
            confidence = RuleCheckModelReply.Confidence.MEDIUM; // HIGH has to be backed by the owner's own words
        }
        return new RuleCheckModelReply.Finding(rule, List.copyOf(entryHandles), reason, quote, confidence);
    }

    /** "J12", "[j12]" and " J12 " are the same handle; anything else is not one. */
    private static String handle(String raw) {
        if (raw == null) return null;
        String h = raw.strip().replace("[", "").replace("]", "").toUpperCase(Locale.ROOT);
        return h.matches("[RJ]\\d{1,12}") ? h : null;
    }

    private static List<RuleFinding> toFindings(RuleCheckModelReply reply, RuleCheckPromptBuilder.Built built) {
        List<RuleFinding> out = new ArrayList<>();
        for (RuleCheckModelReply.Finding f : reply.findings()) {
            List<Long> ids = f.entries().stream().map(h -> built.entries().get(h).entry().id()).toList();
            LocalDate date = f.entries().stream().map(h -> built.entries().get(h).entry().createdAt().toLocalDate())
                    .max(Comparator.naturalOrder()).orElseThrow();
            out.add(RuleFinding.fromModel(date, f.reason(), ids, built.ruleIds().get(f.rule()), f.quote(),
                    f.confidence().name()));
        }
        return out;
    }

    // ---- reading the journal ------------------------------------------------------------------

    private List<RuleCheckPromptBuilder.EntryIn> entries(LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT e.id, e.created_at, e.entry_type, e.mood, e.body,
                       (SELECT coalesce(array_agg(lower(t.name) ORDER BY lower(t.name)), '{}') FROM invest_journal_entry_tags jt
                          JOIN invest_tags t ON t.id = jt.tag_id AND t.deleted_at IS NULL
                         WHERE jt.entry_id = e.id) AS tags
                FROM invest_journal_entries e
                WHERE e.deleted_at IS NULL AND e.created_at >= ? AND e.created_at < ?
                ORDER BY e.created_at DESC, e.id DESC
                LIMIT ?""",
                (rs, i) -> {
                    short mood = rs.getShort("mood");
                    Short moodValue = rs.wasNull() ? null : mood; // read right after the column it describes
                    return new RuleCheckPromptBuilder.EntryIn(rs.getLong("id"), rs.getTimestamp("created_at").toLocalDateTime(),
                            rs.getString("entry_type"), moodValue,
                            List.of((String[]) rs.getArray("tags").getArray()),
                            rs.getString("body"));
                },
                Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.plusDays(1).atStartOfDay()), FETCH_LIMIT);
    }

    private int count(LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM invest_journal_entries WHERE deleted_at IS NULL "
                        + "AND created_at >= ? AND created_at < ?", Integer.class,
                Timestamp.valueOf(from.atStartOfDay()), Timestamp.valueOf(to.plusDays(1).atStartOfDay()));
        return n == null ? 0 : n;
    }
}
