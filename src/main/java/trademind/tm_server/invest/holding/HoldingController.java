package trademind.tm_server.invest.holding;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

import java.util.List;

/** The funds you hold, under /api/v1/invest like everything else in the tracker. */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST + "/holdings")
public class HoldingController {

    private final HoldingService holdings;

    public HoldingController(HoldingService holdings) {
        this.holdings = holdings;
    }

    @GetMapping
    public List<HoldingResponse> list() {
        return holdings.list();
    }

    @GetMapping("/{id}")
    public HoldingResponse get(@PathVariable Long id) {
        return holdings.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HoldingResponse create(@Valid @RequestBody HoldingRequest request) {
        return holdings.create(request);
    }

    @PutMapping("/{id}")
    public HoldingResponse update(@PathVariable Long id, @Valid @RequestBody HoldingRequest request) {
        return holdings.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        holdings.delete(id);
    }
}
