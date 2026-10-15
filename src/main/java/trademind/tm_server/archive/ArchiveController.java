package trademind.tm_server.archive;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.trade.TradeService;

/**
 * Read-only access to the trading history. Deliberately outside the trading prefixes, so it keeps
 * working while {@code features.trading.enabled} is false: hiding trading must never strand the data
 * someone needs for statements or taxes.
 */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_ARCHIVE)
public class ArchiveController {

    private static final String CSV = "text/csv";

    private final TradeService trades;

    public ArchiveController(TradeService trades) {
        this.trades = trades;
    }

    @GetMapping(value = "/trades.csv", produces = CSV)
    public ResponseEntity<String> tradesCsv() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"trades.csv\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(CSV))
                .body(TradeArchiveCsv.export(trades.getAll()));
    }
}
