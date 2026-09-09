package trademind.tm_server.ai;

import org.junit.jupiter.api.Test;
import trademind.tm_server.enums.*;
import trademind.tm_server.trade.Trade;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static trademind.tm_server.ai.AiSuggestionPromptBuilder.*;

// Exercises AiSuggestionService.safeEnum and .fieldsStillNull directly --
// both package-private static methods, so this needs no Spring context and
// no mocking, same testing philosophy as TradeEnrichmentServiceTest. These
// are the two places a hallucinated LLM response, or a trade that's already
// partly filled in by the deterministic importer pass, meet the actual
// suggestion guard logic, so they're worth covering directly.
class AiSuggestionServiceTest {

    @Test
    void exactMatchResolves() {
        assertEquals(ProbabilityEnum.HIGH, AiSuggestionService.safeEnum(ProbabilityEnum.class, "HIGH"));
    }

    @Test
    void caseInsensitiveAndTrimmed() {
        assertEquals(ProbabilityEnum.LOW, AiSuggestionService.safeEnum(ProbabilityEnum.class, "  low  "));
    }

    @Test
    void nullOrBlankYieldsNull() {
        assertNull(AiSuggestionService.safeEnum(ProbabilityEnum.class, null));
        assertNull(AiSuggestionService.safeEnum(ProbabilityEnum.class, "   "));
    }

    @Test
    void hallucinatedValueYieldsNullRatherThanThrowing() {
        assertNull(AiSuggestionService.safeEnum(ProbabilityEnum.class, "VERY_HIGH"));
        assertNull(AiSuggestionService.safeEnum(ProbabilityEnum.class, "not even an enum value"));
    }

    @Test
    void fieldsStillNull_onABlankTradeAsksAboutEveryAiSuggestibleField() {
        Set<String> needed = AiSuggestionService.fieldsStillNull(new Trade());

        assertEquals(
                Set.of(
                        FIELD_ENTRY_REASON, FIELD_MARKET_STATE, FIELD_INVALIDATION_CONDITION,
                        FIELD_PROBABILITY, FIELD_ENTRY_ZONE_TYPE, FIELD_ENTRY_CONFIRMATION,
                        FIELD_EXIT_REASON, FIELD_SL_PLACEMENT_REASON, FIELD_TP_PLACEMENT_REASON,
                        FIELD_MARKET_BIAS
                ),
                needed
        );
    }

    @Test
    void fieldsStillNull_excludesFieldsAlreadyFilledByTheDeterministicImporterPass() {
        // A trade fresh out of the MT5 importer's structure_detection.py /
        // classification_rules.py pass (Phase 5) already has these two set --
        // the AI call must never be asked to re-suggest them.
        Trade trade = new Trade();
        trade.setEntryZoneType(EntryZoneTypeEnum.OB);
        trade.setEntryConfirmation(EntryConfirmationEnum.BOS);

        Set<String> needed = AiSuggestionService.fieldsStillNull(trade);

        assertFalse(needed.contains(FIELD_ENTRY_ZONE_TYPE));
        assertFalse(needed.contains(FIELD_ENTRY_CONFIRMATION));
        assertTrue(needed.contains(FIELD_ENTRY_REASON));
        assertEquals(8, needed.size());
    }

    @Test
    void fieldsStillNull_isEmptyOnceEverySuggestibleFieldIsSet() {
        Trade trade = new Trade();
        trade.setEntryReason(EntryReasonEnum.BREAKOUT);
        trade.setMarketState(MarketStateEnum.UPTREND);
        trade.setInvalidationCondition(InvalidationConditionEnum.SWEEP_FAILURE);
        trade.setProbability(ProbabilityEnum.HIGH);
        trade.setEntryZoneType(EntryZoneTypeEnum.FVG);
        trade.setEntryConfirmation(EntryConfirmationEnum.RETEST);
        trade.setExitReason(ExitReasonEnum.TP_HIT);
        trade.setSlPlacementReason(StopLossPlacementReasonEnum.STRUCTURE);
        trade.setTpPlacementReason(TakeProfitPlacementReasonEnum.FVG_FILL);
        trade.setMarketBias(MarketBiasEnum.BULLISH);

        assertTrue(AiSuggestionService.fieldsStillNull(trade).isEmpty());
    }
}
