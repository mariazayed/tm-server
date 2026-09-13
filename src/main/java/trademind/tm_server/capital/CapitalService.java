package trademind.tm_server.capital;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.base.BaseSearchService;
import trademind.tm_server.base.SearchResponse;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.capital.dto.NewCapitalRequest;
import trademind.tm_server.capital.dto.CapitalResponse;
import trademind.tm_server.capital.dto.CapitalSearchRequest;
import trademind.tm_server.capital.repository.CapitalRepository;
import trademind.tm_server.capital.repository.CapitalSearchRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CapitalService {

    @Autowired
    private CapitalRepository repository;

    @Autowired
    private CapitalSearchRepository searchRepository;

    @Autowired
    private CapitalMapper mapper;

    private final String tableName = "Capital";

    public List<CapitalResponse> create(List<NewCapitalRequest> requests) {
        // Same externalId dedupe as TradeService.create(): the MT5 importer's
        // balance-operation import (deposits/withdrawals) sets externalId to
        // the broker deal ticket, so re-running it over an overlapping date
        // range never double-books a deposit/withdrawal. Manual dashboard
        // entries and the PROFIT/LOSS rows TradeService.create() books
        // alongside a trade never set externalId, so they're always "new".
        List<String> incomingExternalIds = requests.stream()
                .map(NewCapitalRequest::getExternalId)
                .filter(Objects::nonNull)
                .toList();

        Set<String> existingExternalIds = incomingExternalIds.isEmpty()
                ? Set.of()
                : repository.findByExternalIdIn(incomingExternalIds).stream()
                    .map(Capital::getExternalId)
                    .collect(Collectors.toSet());

        List<NewCapitalRequest> newRequests = filterNewRequests(requests, existingExternalIds);

        if (newRequests.isEmpty()) {
            return List.of();
        }

        List<Capital> capitalEntities = newRequests.stream()
                .map(mapper::toEntity)
                .toList();

        List<Capital> savedCapitals = repository.saveAll(capitalEntities);

        return savedCapitals.stream()
                .map(mapper::toResponse)
                .toList();
    }

    // Pure filter, extracted for direct unit testing without a Spring context
    // or a real repository -- mirrors TradeService.filterNewRequests exactly.
    // A request survives if it either has no externalId (manual entry, or a
    // PROFIT/LOSS row derived from a trade) or its externalId isn't among the
    // ones the batch dedupe query already found in the DB.
    static List<NewCapitalRequest> filterNewRequests(List<NewCapitalRequest> requests, Set<String> existingExternalIds) {
        return requests.stream()
                .filter(request -> request.getExternalId() == null
                        || !existingExternalIds.contains(request.getExternalId()))
                .toList();
    }

    public List<CapitalResponse> getAll() {
        List<Capital> capitalPlanList = repository.findByDeletedAtIsNull();

        return capitalPlanList.stream()
                .map(mapper::toResponse) // convert each Trade to TradeResponse
                .toList();
    }

    public CapitalResponse getOneById(Long id) {
        Capital capitalPlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(capitalPlan);
    }

    // Real, server-side pagination -- a Capital row is booked for every
    // deposit/withdraw AND every trade's PROFIT/LOSS, so this list grows
    // with trade volume too. The Deposits & Withdraws screen pages through
    // this instead of getAll() pulling the entire history into memory.
    public SearchResponse<CapitalResponse> search(CapitalSearchRequest request) {
        Map<String, Object> filters = new HashMap<>();
        if (request.getReasons() != null && !request.getReasons().isEmpty()) {
            filters.put("reason", request.getReasons());
        }
        Map<String, String> relationFilters = new HashMap<>();

        BaseSearchService<Capital> baseService = new BaseSearchService<>(searchRepository);

        return baseService.search(request, filters, relationFilters, mapper::toResponse);
    }
}