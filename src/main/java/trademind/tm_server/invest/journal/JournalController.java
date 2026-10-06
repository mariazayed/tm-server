package trademind.tm_server.invest.journal;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.enums.JournalEntryType;
import trademind.tm_server.invest.journal.JournalDtos.CheckinTemplate;
import trademind.tm_server.invest.journal.JournalDtos.EntryRequest;
import trademind.tm_server.invest.journal.JournalDtos.EntryResponse;
import trademind.tm_server.invest.journal.JournalDtos.PageResponse;
import trademind.tm_server.invest.journal.JournalDtos.TagResponse;

import java.time.LocalDate;
import java.util.List;

/** Under /api/v1/invest, so every route needs a valid login token. */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/journal")
public class JournalController {

    private final JournalService service;

    public JournalController(JournalService service) {
        this.service = service;
    }

    /** Timeline, newest first. {@code q} is a case-insensitive substring match on the body. */
    @GetMapping
    public PageResponse list(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) JournalEntryType type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return service.list(new JournalService.Filter(tag, type, q, from, to), page, size);
    }

    @GetMapping("/tags")
    public List<TagResponse> tags() {
        return service.tags();
    }

    @GetMapping("/checkin-template")
    public CheckinTemplate checkinTemplate() {
        return service.checkinTemplate();
    }

    @GetMapping("/{id}")
    public EntryResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntryResponse create(@Valid @RequestBody EntryRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public EntryResponse update(@PathVariable Long id, @Valid @RequestBody EntryRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
