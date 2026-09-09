package trademind.tm_server.ai;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import trademind.tm_server.enums.*;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ConflictException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.trade.Trade;
import trademind.tm_server.trade.TradeMapper;
import trademind.tm_server.trade.dto.TradeResponse;
import trademind.tm_server.trade.repository.TradeRepository;

import java.util.LinkedHashSet;
import java.util.Set;

import static trademind.tm_server.ai.AiSuggestionPromptBuilder.*;

/**
 * Explicit, cost-gated AI-suggestion pass for the analytical fields that
 * are still null after a trade's import/creation -- see the Phase 5 notes
 * in the project plan. Triggered only by POST /trade/{id}/suggest (never
 * automatically), and guarded two ways:
 *
 *   1. Never touches a trade that's already been reviewed (reviewedAt set)
 *      -- a confirmed trade is done, full stop.
 *   2. Only ever asks about, and only ever writes, fields that are
 *      currently null -- never overwrites anything already set, whether
 *      that came from a human, the MT5 importer's deterministic pass
 *      (classification_rules.py), or an earlier call to this same
 *      endpoint. This is checked once to decide what to ask for and again
 *      before each individual field is written, so it holds even if the
 *      trade changed between those two points.
 */
@Service
public class AiSuggestionService {

    @Autowired
    private TradeRepository repository;

    @Autowired
    private TradeMapper mapper;

    @Autowired
    private LlmClient llmClient;

    @Value("${ai.suggestions.enabled:false}")
    private boolean enabled;

    private final String tableName = "Trade";

    public TradeResponse suggest(Long id) {
        Trade trade = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (trade.getReviewedAt() != null) {
            throw new ConflictException(
                    "Trade " + id + " has already been reviewed -- AI suggestions never overwrite a confirmed trade."
            );
        }

        Set<String> neededFields = fieldsStillNull(trade);
        if (neededFields.isEmpty()) {
            // Every suggestible field is already set -- nothing to do,
            // regardless of whether AI suggestions are even enabled.
            return mapper.toResponse(trade);
        }

        if (!enabled) {
            throw new BadRequestException(
                    "AI suggestions are disabled -- set ai.suggestions.enabled=true and " +
                    "ai.provider.api-key in application-local.properties to turn this on."
            );
        }

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(trade, neededFields);

        String rawResponse;
        try {
            rawResponse = llmClient.complete(systemPrompt, userPrompt);
        } catch (LlmException e) {
            throw new IllegalStateException("AI suggestion request failed: " + e.getMessage(), e);
        }

        AiSuggestion suggestion = AiSuggestionResponseParser.parse(rawResponse);
        applySuggestion(trade, suggestion);

        Trade saved = repository.save(trade);
        return mapper.toResponse(saved);
    }

    /** Every suggestible field that's currently null -- exactly what gets
     * asked about below. Package-private static so AiSuggestionServiceTest
     * can exercise it directly with a plain Trade, no Spring context or
     * repository needed. */
    static Set<String> fieldsStillNull(Trade trade) {
        Set<String> needed = new LinkedHashSet<>();
        if (trade.getEntryReason() == null) needed.add(FIELD_ENTRY_REASON);
        if (trade.getMarketState() == null) needed.add(FIELD_MARKET_STATE);
        if (trade.getInvalidationCondition() == null) needed.add(FIELD_INVALIDATION_CONDITION);
        if (trade.getProbability() == null) needed.add(FIELD_PROBABILITY);
        if (trade.getEntryZoneType() == null) needed.add(FIELD_ENTRY_ZONE_TYPE);
        if (trade.getEntryConfirmation() == null) needed.add(FIELD_ENTRY_CONFIRMATION);
        if (trade.getExitReason() == null) needed.add(FIELD_EXIT_REASON);
        if (trade.getSlPlacementReason() == null) needed.add(FIELD_SL_PLACEMENT_REASON);
        if (trade.getTpPlacementReason() == null) needed.add(FIELD_TP_PLACEMENT_REASON);
        if (trade.getMarketBias() == null) needed.add(FIELD_MARKET_BIAS);
        return needed;
    }

    /** Re-checks null-ness per field before writing (not just relying on
     * fieldsStillNull having been computed a moment earlier) -- this is the
     * actual safety guarantee, not an optimization. */
    private void applySuggestion(Trade trade, AiSuggestion suggestion) {
        if (trade.getEntryReason() == null) {
            trade.setEntryReason(safeEnum(EntryReasonEnum.class, suggestion.entryReason()));
        }
        if (trade.getMarketState() == null) {
            trade.setMarketState(safeEnum(MarketStateEnum.class, suggestion.marketState()));
        }
        if (trade.getInvalidationCondition() == null) {
            trade.setInvalidationCondition(safeEnum(InvalidationConditionEnum.class, suggestion.invalidationCondition()));
        }
        if (trade.getProbability() == null) {
            trade.setProbability(safeEnum(ProbabilityEnum.class, suggestion.probability()));
        }
        if (trade.getEntryZoneType() == null) {
            trade.setEntryZoneType(safeEnum(EntryZoneTypeEnum.class, suggestion.entryZoneType()));
        }
        if (trade.getEntryConfirmation() == null) {
            trade.setEntryConfirmation(safeEnum(EntryConfirmationEnum.class, suggestion.entryConfirmation()));
        }
        if (trade.getExitReason() == null) {
            trade.setExitReason(safeEnum(ExitReasonEnum.class, suggestion.exitReason()));
        }
        if (trade.getSlPlacementReason() == null) {
            trade.setSlPlacementReason(safeEnum(StopLossPlacementReasonEnum.class, suggestion.slPlacementReason()));
        }
        if (trade.getTpPlacementReason() == null) {
            trade.setTpPlacementReason(safeEnum(TakeProfitPlacementReasonEnum.class, suggestion.tpPlacementReason()));
        }
        if (trade.getMarketBias() == null) {
            trade.setMarketBias(safeEnum(MarketBiasEnum.class, suggestion.marketBias()));
        }
    }

    /** Case-insensitive, exception-safe Enum.valueOf -- a hallucinated or
     * misspelled value from the model degrades to "no suggestion" (null)
     * instead of throwing and failing the whole request. Package-private
     * so AiSuggestionServiceTest can exercise it directly without a Spring
     * context. */
    static <E extends Enum<E>> E safeEnum(Class<E> type, String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
