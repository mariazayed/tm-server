package trademind.tm_server.invest.rules;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.invest.Rule;
import trademind.tm_server.invest.repository.RuleRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** The written strategy: a short ordered list of rules shown on the dashboard. */
@Service
@Transactional
public class RulesService {

    public static final int MAX_RULES = 20;

    public record RuleRequest(@NotBlank @Size(max = 500) String text) {
    }

    public record OrderRequest(@NotEmpty List<@NotNull Long> ids) {
    }

    public record RuleResponse(Long id, String text, int position) {
    }

    private final RuleRepository rules;
    private final Clock clock;

    public RulesService(RuleRepository rules, Clock clock) {
        this.rules = rules;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<RuleResponse> list() {
        return responses(rules.findByDeletedAtIsNullOrderBySortOrderAsc());
    }

    public RuleResponse create(RuleRequest request) {
        List<Rule> live = rules.findByDeletedAtIsNullOrderBySortOrderAsc();
        if (live.size() >= MAX_RULES) {
            throw new BadRequestException("At most " + MAX_RULES + " rules; remove one first");
        }
        Rule rule = new Rule();
        rule.setText(clean(request.text()));
        rule.setSortOrder(live.isEmpty() ? 1 : live.get(live.size() - 1).getSortOrder() + 1);
        Rule saved = rules.save(rule);
        return new RuleResponse(saved.getId(), saved.getText(), live.size() + 1);
    }

    public RuleResponse update(Long id, RuleRequest request) {
        Rule rule = find(id);
        rule.setText(clean(request.text()));
        rules.save(rule);
        return list().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
    }

    public void delete(Long id) {
        Rule rule = find(id);
        rule.setDeletedAt(LocalDateTime.now(clock));
        rules.save(rule);
    }

    /** Replaces the order. {@code ids} must be exactly the current rules, each once. */
    public List<RuleResponse> reorder(OrderRequest request) {
        List<Rule> live = rules.findByDeletedAtIsNullOrderBySortOrderAsc();
        Map<Long, Rule> byId = live.stream().collect(Collectors.toMap(Rule::getId, Function.identity()));
        if (request.ids().size() != live.size() || !new HashSet<>(request.ids()).equals(byId.keySet())) {
            throw new BadRequestException("The order must list every current rule exactly once");
        }
        int position = 1;
        for (Long id : request.ids()) {
            byId.get(id).setSortOrder(position++);
        }
        rules.saveAll(live);
        return list();
    }

    private static String clean(String text) {
        String t = text.strip();
        if (t.isEmpty()) throw new BadRequestException("A rule needs some text");
        return t;
    }

    private Rule find(Long id) {
        return rules.findById(id).filter(r -> r.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Rule " + id + " not found"));
    }

    private static List<RuleResponse> responses(List<Rule> live) {
        return java.util.stream.IntStream.range(0, live.size())
                .mapToObj(i -> new RuleResponse(live.get(i).getId(), live.get(i).getText(), i + 1)).toList();
    }
}
