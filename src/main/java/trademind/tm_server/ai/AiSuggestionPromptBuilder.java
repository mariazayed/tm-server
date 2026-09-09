package trademind.tm_server.ai;

import trademind.tm_server.enums.*;
import trademind.tm_server.trade.Trade;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builds the two prompt strings sent to the LLM for AiSuggestionService.
 * Pure/static and Spring-free on purpose -- see AiSuggestionPromptBuilderTest
 * for how this is exercised without any HTTP call or Spring context.
 *
 * Per the implementation plan, this is only ever asked to fill fields that
 * genuinely require weighing multiple signals together (entryReason,
 * marketState, invalidationCondition, probability), plus -- as a fallback
 * only -- whichever of the geometrically-derivable fields
 * (entryZoneType/entryConfirmation/exitReason/slPlacementReason/
 * tpPlacementReason/marketBias) the importer's deterministic pass in
 * classification_rules.py couldn't determine from price structure alone.
 * AiSuggestionService decides which fields are actually "needed" (still
 * null) and passes that set in here.
 */
public final class AiSuggestionPromptBuilder {

    private AiSuggestionPromptBuilder() {
    }

    public static final String FIELD_ENTRY_REASON = "entryReason";
    public static final String FIELD_MARKET_STATE = "marketState";
    public static final String FIELD_INVALIDATION_CONDITION = "invalidationCondition";
    public static final String FIELD_PROBABILITY = "probability";
    public static final String FIELD_ENTRY_ZONE_TYPE = "entryZoneType";
    public static final String FIELD_ENTRY_CONFIRMATION = "entryConfirmation";
    public static final String FIELD_EXIT_REASON = "exitReason";
    public static final String FIELD_SL_PLACEMENT_REASON = "slPlacementReason";
    public static final String FIELD_TP_PLACEMENT_REASON = "tpPlacementReason";
    public static final String FIELD_MARKET_BIAS = "marketBias";

    public static String buildSystemPrompt() {
        return """
                You are an assistant embedded in TradeMind, a personal trading journal \
                that uses SMC/ICT (Smart Money Concepts / Inner Circle Trader) terminology: \
                order blocks (OB), fair value gaps (FVG), break of structure (BOS), change \
                of character (CHoCH), and liquidity sweeps.

                You will be given the facts of one already-closed trade -- its direction, \
                prices, timing, and a JSON list of price-structure events that were detected \
                deterministically from price data before you were called (you did not detect \
                these yourself; treat them as given facts, not something to second-guess).

                Your only job is to classify a specific, named set of fields for this trade, \
                each from a fixed list of allowed values given to you per field. Only use \
                information present in what you're given -- do not invent price levels, \
                times, or outcomes not stated.

                Respond with ONLY a single JSON object, no markdown code fences, no \
                explanation before or after it. Use exactly the field names you were asked \
                for as JSON keys. For any field you are not reasonably confident about, use \
                JSON null for that field's value rather than guessing -- a wrong guess that \
                looks confident is worse than an honest "unknown" here, since a person will \
                review every suggestion before it's kept.
                """;
    }

    public static String buildUserPrompt(Trade trade, Set<String> neededFields) {
        StringBuilder sb = new StringBuilder();

        sb.append("Trade facts:\n");
        sb.append("- direction: ").append(trade.getDirection()).append('\n');
        sb.append("- entryPrice: ").append(trade.getEntryPrice()).append('\n');
        sb.append("- exitPrice: ").append(trade.getExitPrice()).append('\n');
        sb.append("- slPrice: ").append(trade.getSlPrice()).append('\n');
        sb.append("- tpPrice: ").append(trade.getTpPrice()).append('\n');
        sb.append("- entryDateTime (UTC): ").append(trade.getEntryDateTime()).append('\n');
        sb.append("- exitDateTime (UTC): ").append(trade.getExitDateTime()).append('\n');
        sb.append("- lotSize: ").append(trade.getLotSize()).append('\n');
        sb.append("- pnl: ").append(trade.getPnl()).append('\n');
        appendIfPresent(sb, "session", trade.getSession());
        appendIfPresent(sb, "dayOfWeek", trade.getDayOfWeek());
        appendIfPresent(sb, "tradeType", trade.getTradeType());
        appendIfPresent(sb, "volatility", trade.getVolatility());
        appendIfPresent(sb, "rMultiple", trade.getRMultiple());
        // Already-known analytical fields (from the deterministic import-time
        // pass, or a previous partial suggestion) are given as context even
        // when not in neededFields -- they help the model reason about the
        // fields it IS being asked for, and must never be re-suggested since
        // they're already set.
        appendIfPresent(sb, "entryZoneType (already determined -- for context only)", trade.getEntryZoneType());
        appendIfPresent(sb, "entryConfirmation (already determined -- for context only)", trade.getEntryConfirmation());
        appendIfPresent(sb, "exitReason (already determined -- for context only)", trade.getExitReason());
        appendIfPresent(sb, "slPlacementReason (already determined -- for context only)", trade.getSlPlacementReason());
        appendIfPresent(sb, "tpPlacementReason (already determined -- for context only)", trade.getTpPlacementReason());
        appendIfPresent(sb, "marketBias (already determined -- for context only)", trade.getMarketBias());

        sb.append("\nDetected price structures (chronological JSON list; may be empty if none were found):\n");
        sb.append(trade.getDetectedStructures() != null ? trade.getDetectedStructures() : "[]").append('\n');

        sb.append("\nFill in exactly these fields, each restricted to its listed allowed values ")
          .append("(or null if not confident):\n");

        if (neededFields.contains(FIELD_ENTRY_REASON)) {
            appendFieldSpec(sb, FIELD_ENTRY_REASON,
                    "the higher-level reason this entry was taken", EntryReasonEnum.class);
        }
        if (neededFields.contains(FIELD_MARKET_STATE)) {
            appendFieldSpec(sb, FIELD_MARKET_STATE,
                    "the overall market condition at entry (trend/consolidation/volatility/volume -- pick the single most decision-relevant one)",
                    MarketStateEnum.class);
        }
        if (neededFields.contains(FIELD_INVALIDATION_CONDITION)) {
            appendFieldSpec(sb, FIELD_INVALIDATION_CONDITION,
                    "what would have proven this trade's setup wrong (most relevant for losing trades)",
                    InvalidationConditionEnum.class);
        }
        if (neededFields.contains(FIELD_PROBABILITY)) {
            appendFieldSpec(sb, FIELD_PROBABILITY,
                    "your holistic confidence in this setup based on how many structural signals aligned",
                    ProbabilityEnum.class);
        }
        if (neededFields.contains(FIELD_ENTRY_ZONE_TYPE)) {
            appendFieldSpec(sb, FIELD_ENTRY_ZONE_TYPE,
                    "the type of zone the entry price was in (a deterministic pass already tried and couldn't determine this from structure alone)",
                    EntryZoneTypeEnum.class);
        }
        if (neededFields.contains(FIELD_ENTRY_CONFIRMATION)) {
            appendFieldSpec(sb, FIELD_ENTRY_CONFIRMATION,
                    "what confirmed the entry (a deterministic pass already tried and couldn't determine this)",
                    EntryConfirmationEnum.class);
        }
        if (neededFields.contains(FIELD_EXIT_REASON)) {
            appendFieldSpec(sb, FIELD_EXIT_REASON,
                    "why the trade was closed (a deterministic pass already tried and couldn't determine this)",
                    ExitReasonEnum.class);
        }
        if (neededFields.contains(FIELD_SL_PLACEMENT_REASON)) {
            appendFieldSpec(sb, FIELD_SL_PLACEMENT_REASON,
                    "why the stop-loss was placed where it was (a deterministic pass already tried and couldn't determine this)",
                    StopLossPlacementReasonEnum.class);
        }
        if (neededFields.contains(FIELD_TP_PLACEMENT_REASON)) {
            appendFieldSpec(sb, FIELD_TP_PLACEMENT_REASON,
                    "why the take-profit was placed where it was (a deterministic pass already tried and couldn't determine this)",
                    TakeProfitPlacementReasonEnum.class);
        }
        if (neededFields.contains(FIELD_MARKET_BIAS)) {
            appendFieldSpec(sb, FIELD_MARKET_BIAS,
                    "the prevailing directional bias at entry (a deterministic pass already tried and couldn't determine this)",
                    MarketBiasEnum.class);
        }

        return sb.toString();
    }

    private static void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value != null) {
            sb.append("- ").append(label).append(": ").append(value).append('\n');
        }
    }

    private static <E extends Enum<E>> void appendFieldSpec(StringBuilder sb, String fieldName,
                                                              String description, Class<E> enumType) {
        sb.append("- \"").append(fieldName).append("\": ").append(description)
          .append(". Allowed values: [").append(enumOptions(enumType)).append("]\n");
    }

    private static <E extends Enum<E>> String enumOptions(Class<E> enumType) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }

    /** All ten field names this builder knows how to ask about, in the order
     * they should be checked/applied -- shared with AiSuggestionService so
     * the two stay in sync without duplicating the list. */
    public static Set<String> allSuggestibleFields() {
        return new LinkedHashSet<>(Arrays.asList(
                FIELD_ENTRY_REASON, FIELD_MARKET_STATE, FIELD_INVALIDATION_CONDITION, FIELD_PROBABILITY,
                FIELD_ENTRY_ZONE_TYPE, FIELD_ENTRY_CONFIRMATION, FIELD_EXIT_REASON,
                FIELD_SL_PLACEMENT_REASON, FIELD_TP_PLACEMENT_REASON, FIELD_MARKET_BIAS
        ));
    }
}
