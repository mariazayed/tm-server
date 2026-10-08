package trademind.tm_server.invest.ai;

import org.springframework.stereotype.Service;
import trademind.tm_server.invest.TagService;
import trademind.tm_server.invest.repository.InvestTagRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Suggests tags for a journal entry, which may still be an unsaved draft. Suggestion-only: nothing is
 * created or saved here. A tag becomes real only when the owner keeps the chip and saves the entry.
 * The model's tags are normalised the same way the journal does, tags the entry already has are dropped,
 * at most {@value #MAX_NEW} tags that are not already in the owner's vocabulary are kept, and the total
 * respects the per-entry tag limit.
 */
@Service
public class TagSuggestionsService {

    static final int MAX_SUGGESTIONS = 5;
    static final int MAX_NEW = 2;
    static final int MAX_TAG_CHARS = 40;
    static final int MAX_REASON_CHARS = 120;
    private static final Pattern TAG_SHAPE = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N} _-]*");

    /** @param isNew true when the owner has no such tag yet */
    public record Suggestion(String kind, String tag, boolean isNew, String reason) {
    }

    public record Response(List<Suggestion> suggestions) {
    }

    private final InvestTagRepository tags;
    private final AiJsonService json;
    private final AiService ai;

    public TagSuggestionsService(InvestTagRepository tags, AiJsonService json, AiService ai) {
        this.tags = tags;
        this.json = json;
        this.ai = ai;
    }

    /**
     * @throws AiUnavailableException, AiLimitExceededException, AiUpstreamException, AiInvalidReplyException
     *         with the usual 503, 429 and 502 mapping: there is no exact fallback for this feature
     */
    public Response suggest(String body, String entryType, Integer mood, List<String> entryTags) {
        List<String> have = TagService.normalize(entryTags);
        int room = TagService.MAX_TAGS - have.size();
        if (room <= 0) return new Response(List.of()); // already full; nothing could be added, so nothing is sent
        ai.requireAvailable();
        List<InvestTagRepository.TagUsage> usage = tags.findUsage();
        List<TagPromptBuilder.Known> vocabulary = vocabulary(usage);
        AiPromptAssembler.Prompt prompt = TagPromptBuilder.build(body, entryType, mood, have, vocabulary);
        // Every tag the owner has, not just the top 50 that were shown: a tag they have is not new.
        Set<String> known = new LinkedHashSet<>();
        for (InvestTagRepository.TagUsage u : usage) known.add(u.getName().toLowerCase(Locale.ROOT));
        AiJsonService.Result<TagReply> result = json.call(AiFeature.TAG_SUGGESTIONS, prompt.system(), prompt.user(),
                TagReply.class, reply -> clean(reply, have, known, Math.min(MAX_SUGGESTIONS, room)));
        List<Suggestion> out = result.value().tags().stream()
                .map(i -> new Suggestion("TAG", i.tag(), !known.contains(i.tag()), i.reason())).toList();
        return new Response(out);
    }

    /** The owner's most used tags first (ties by name), at most {@value TagPromptBuilder#MAX_VOCABULARY}; unused tags are left out. */
    private static List<TagPromptBuilder.Known> vocabulary(List<InvestTagRepository.TagUsage> usage) {
        return usage.stream()
                .filter(u -> u.getCount() > 0)
                .map(u -> new TagPromptBuilder.Known(u.getName(), u.getCount()))
                .sorted(Comparator.comparingLong(TagPromptBuilder.Known::count).reversed()
                        .thenComparing(TagPromptBuilder.Known::name))
                .limit(TagPromptBuilder.MAX_VOCABULARY).toList();
    }

    /**
     * Cleans the model's tags; an empty list is a valid answer. Only a malformed reply asks for a repeat.
     */
    static TagReply clean(TagReply reply, List<String> have, Set<String> known, int limit) {
        if (reply.tags() == null) throw new ModelJsonException("the tags list was missing");
        List<TagReply.Item> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>(have);
        int newCount = 0;
        for (TagReply.Item item : reply.tags()) {
            if (item == null || item.tag() == null) continue;
            String tag = normaliseOne(item.tag());
            if (tag == null || !seen.add(tag)) continue;
            boolean isNew = !known.contains(tag);
            if (isNew && newCount >= MAX_NEW) continue;
            String reason = item.reason() == null ? "" : item.reason().replaceAll("\\s+", " ").strip();
            if (ChatAnswerCheck.givesAdvice(reason)) continue;
            if (reason.length() > MAX_REASON_CHARS) reason = reason.substring(0, MAX_REASON_CHARS - 1).stripTrailing() + "…";
            if (isNew) newCount++;
            out.add(new TagReply.Item(tag, reason));
            if (out.size() >= limit) break;
        }
        return new TagReply(List.copyOf(out));
    }

    /** The journal's own normalisation, plus a shape check; null when the text is not a usable tag. */
    private static String normaliseOne(String raw) {
        List<String> n = TagService.normalize(List.of(raw));
        if (n.isEmpty()) return null;
        String tag = n.get(0);
        if (tag.length() > MAX_TAG_CHARS || !TAG_SHAPE.matcher(tag).matches()) return null;
        return tag;
    }
}
