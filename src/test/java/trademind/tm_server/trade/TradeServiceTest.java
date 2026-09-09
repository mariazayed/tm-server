package trademind.tm_server.trade;

import org.junit.jupiter.api.Test;
import trademind.tm_server.trade.dto.NewTradeRequest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Exercises TradeService.filterNewRequests directly -- a package-private
// static method, so this needs no Spring context, no mocking, and no real
// database (same testing philosophy as TradeEnrichmentServiceTest and
// AiSuggestionService.fieldsStillNull). This is the actual mechanism behind
// "re-running an overlapping MT5 import range is safe" (Phase 7's plan
// explicitly calls out "dedupe-on-overlapping-import" as something to
// cover): create() runs this filter against whatever externalIds the batch
// query already found in the DB, before anything else -- Capital rows
// included -- gets created for the survivors.
class TradeServiceTest {

    private static NewTradeRequest requestWithExternalId(String externalId) {
        NewTradeRequest request = new NewTradeRequest();
        request.setExternalId(externalId);
        return request;
    }

    @Test
    void requestsWithNoExternalIdAlwaysSurvive() {
        // Manual entries from the app never set externalId -- dedupe must
        // never touch them.
        NewTradeRequest manual1 = requestWithExternalId(null);
        NewTradeRequest manual2 = requestWithExternalId(null);

        List<NewTradeRequest> result = TradeService.filterNewRequests(
                List.of(manual1, manual2), Set.of("some-other-external-id")
        );

        assertEquals(List.of(manual1, manual2), result);
    }

    @Test
    void requestsWithAnAlreadyImportedExternalIdAreDropped() {
        NewTradeRequest duplicate = requestWithExternalId("mt5-position-100");

        List<NewTradeRequest> result = TradeService.filterNewRequests(
                List.of(duplicate), Set.of("mt5-position-100")
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void requestsWithANewExternalIdSurvive() {
        NewTradeRequest fresh = requestWithExternalId("mt5-position-200");

        List<NewTradeRequest> result = TradeService.filterNewRequests(
                List.of(fresh), Set.of("mt5-position-100")
        );

        assertEquals(List.of(fresh), result);
    }

    @Test
    void reimportingAnOverlappingRangeKeepsOnlyTheGenuinelyNewPositions() {
        // Simulates exactly the scenario the plan calls out: an importer run
        // for --from/--to a range that overlaps a previous run. positions
        // 100 and 101 were already imported (present in existingExternalIds,
        // as if returned by TradeRepository.findByExternalIdIn); 102 is new.
        NewTradeRequest alreadyImported1 = requestWithExternalId("100");
        NewTradeRequest alreadyImported2 = requestWithExternalId("101");
        NewTradeRequest genuinelyNew = requestWithExternalId("102");
        NewTradeRequest manualEntry = requestWithExternalId(null);

        List<NewTradeRequest> result = TradeService.filterNewRequests(
                List.of(alreadyImported1, alreadyImported2, genuinelyNew, manualEntry),
                Set.of("100", "101")
        );

        assertEquals(List.of(genuinelyNew, manualEntry), result);
    }

    @Test
    void emptyBatchStaysEmpty() {
        assertTrue(TradeService.filterNewRequests(List.of(), Set.of()).isEmpty());
    }
}
