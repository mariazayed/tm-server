package trademind.tm_server.staticData;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.staticData.dto.StaticDataResponse;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_STATIC_DATA)
public class StaticDataController {
    @GetMapping
    public StaticDataResponse getAll() {
        return StaticDataResponse.builder()
                .tradeDirections(StaticData.TRADE_DIRECTIONS)
                .marketBiases(StaticData.MARKET_BIASES)
                .marketStates(StaticData.MARKET_STATES)
                .entryReasons(StaticData.ENTRY_REASONS)
                .entryConfirmations(StaticData.ENTRY_CONFIRMATIONS)
                .entryZoneTypes(StaticData.ENTRY_ZONE_TYPES)
                .invalidationConditions(StaticData.INVALIDATION_CONDITIONS)
                .probabilities(StaticData.PROBABILITIES)
                .volatilities(StaticData.VOLATILITIES)
                .scalingRules(StaticData.SCALING_RULES)
                .boostRules(StaticData.BOOST_RULES)
                .stopLossPlacementReasons(StaticData.STOP_LOSS_PLACEMENT_REASONS)
                .takeProfitPlacementReasons(StaticData.TAKE_PROFIT_PLACEMENT_REASONS)
                .entryTypes(StaticData.ENTRY_TYPES)
                .sessions(StaticData.SESSIONS)
                .tradeTags(StaticData.TRADE_TAGS)
                .tradeUpdates(StaticData.TRADE_UPDATES)
                .exitReasons(StaticData.EXIT_REASONS)
                .build();

    }
}