package trademind.tm_server.invest.backup;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Under /api/v1/invest, so a login token is required. */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/export")
public class ExportController {

    private final ExportService export;
    private final Clock clock;

    public ExportController(ExportService export, Clock clock) {
        this.export = export;
        this.clock = clock;
    }

    /** One JSON document with everything the tracker stores. */
    @GetMapping
    public ResponseEntity<ExportData> export() {
        String name = "invest-export-" + LocalDate.now(clock.withZone(ZoneOffset.UTC)) + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(export.export());
    }
}
