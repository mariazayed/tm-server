package trademind.tm_server.strictRules;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.strictRules.dto.NewStrictRuleRequest;
import trademind.tm_server.strictRules.dto.StrictRuleResponse;
import trademind.tm_server.strictRules.repository.StrictRuleRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderNoteService {

    @Autowired
    private StrictRuleRepository repository;

    @Autowired
    private ReminderNoteMapper mapper;

    private final String tableName = "Strict Rules";

    public List<StrictRuleResponse> create(List<NewStrictRuleRequest> requests) {
        List<StrictRule> strictRuleEntities = requests.stream()
                .map(mapper::toEntity)
                .toList();

        List<StrictRule> savedStrictRules = repository.saveAll(strictRuleEntities);

        return savedStrictRules.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public List<StrictRuleResponse> getAll() {
        List<StrictRule> strictRulePlanList = repository.findByDeletedAtIsNull();

        return strictRulePlanList.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public StrictRuleResponse getOneById(Long id) {
        StrictRule strictRulePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(strictRulePlan);
    }

    public void delete(Long id) {
        StrictRule object = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        object.setDeletedAt(LocalDateTime.now());
        repository.save(object);
    }
}