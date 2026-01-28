package trademind.tm_server.trade;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.trade.dto.NewTradeRequest;
import trademind.tm_server.trade.dto.TradeResponse;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_TRADE)
public class TradeController {
    @Autowired
    private TradeService service;

    @PostMapping
    public List<TradeResponse> create(@RequestBody List<NewTradeRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<TradeResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public TradeResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }
}