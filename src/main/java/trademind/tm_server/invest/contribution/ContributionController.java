package trademind.tm_server.invest.contribution;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.invest.dto.ContributionRequest;
import trademind.tm_server.invest.dto.ContributionResponse;
import trademind.tm_server.invest.dto.ImportResult;
import trademind.tm_server.invest.dto.InvestSettingsResponse;

import java.util.List;

/** All routes sit under /api/v1/invest, so they require a valid login token. */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST)
public class ContributionController {

    private static final String CSV = "text/csv";

    private final ContributionService service;

    public ContributionController(ContributionService service) {
        this.service = service;
    }

    @GetMapping("/settings")
    public InvestSettingsResponse settings() {
        return service.getSettings();
    }

    @GetMapping("/contributions")
    public List<ContributionResponse> list() {
        return service.list();
    }

    @GetMapping("/contributions/{id}")
    public ContributionResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    public ContributionResponse create(@Valid @RequestBody ContributionRequest request) {
        return service.create(request);
    }

    @PutMapping("/contributions/{id}")
    public ContributionResponse update(@PathVariable Long id, @Valid @RequestBody ContributionRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/contributions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/contributions/status")
    public StatusResponse status() {
        return StatusResponse.from(service.status());
    }

    @GetMapping(value = "/contributions/export", produces = CSV)
    public ResponseEntity<String> export() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"contributions.csv\"")
                .contentType(MediaType.parseMediaType(CSV))
                .body(service.exportCsv());
    }

    @PostMapping(value = "/contributions/import", consumes = {CSV, MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<ImportResult> importCsv(@RequestBody String csv) {
        ImportResult result = service.importCsv(csv);
        return ResponseEntity.status(result.errors().isEmpty() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY)
                .body(result);
    }
}
