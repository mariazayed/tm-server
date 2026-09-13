package trademind.tm_server.capital;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.capital.dto.NewCapitalRequest;
import trademind.tm_server.capital.dto.CapitalResponse;
import trademind.tm_server.capital.dto.CapitalSearchRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_CAPITAL)
public class CapitalController {
    @Autowired
    private CapitalService service;

    @PostMapping
    public List<CapitalResponse> create(@RequestBody List<NewCapitalRequest> requests) {
        return service.create(requests);
    }

    @GetMapping
    public List<CapitalResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public CapitalResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PostMapping(AppConstants.SUB_URL_SEARCH)
    public SearchResponse<CapitalResponse> search(@RequestBody CapitalSearchRequest request) {
        return service.search(request);
    }
}