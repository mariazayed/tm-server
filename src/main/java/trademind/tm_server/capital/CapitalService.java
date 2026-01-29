package trademind.tm_server.capital;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.capital.dto.NewCapitalRequest;
import trademind.tm_server.capital.dto.CapitalResponse;
import trademind.tm_server.capital.repository.CapitalRepository;

import java.util.List;

@Service
public class CapitalService {

    @Autowired
    private CapitalRepository repository;

    @Autowired
    private CapitalMapper mapper;

    private final String tableName = "Capital";

    public List<CapitalResponse> create(List<NewCapitalRequest> requests) {
        List<Capital> capitalEntities = requests.stream()
                .map(mapper::toEntity)
                .toList();

        List<Capital> savedCapitals = repository.saveAll(capitalEntities);

        return savedCapitals.stream()
                .map(mapper::toResponse)
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
}