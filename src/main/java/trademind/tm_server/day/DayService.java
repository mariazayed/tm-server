package trademind.tm_server.day;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.day.dto.DayResponse;
import trademind.tm_server.day.dto.NewDayRequest;
import trademind.tm_server.day.dto.UpdateDayRequest;
import trademind.tm_server.day.repository.DayRepository;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DayService {

    @Autowired
    private DayRepository repository;

    @Autowired
    private DayMapper mapper;

    private final String tableName = "Day";

    public DayResponse create(NewDayRequest trade) {
        Day tradePlanEntity = mapper.toEntity(trade);
        Day savedDay = repository.save(tradePlanEntity);
        return mapper.toResponse(savedDay);
    }

    public List<DayResponse> getAll() {
        List<Day> tradePlanList = repository.findByDeletedAtIsNull();

        return tradePlanList.stream()
                .map(mapper::toResponse) // convert each Trade to TradeTerminateResponse
                .toList();
    }

    public DayResponse getOneById(Long id) {
        Day tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(tradePlan);
    }

    public Day getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public DayResponse update(Long id, UpdateDayRequest tradeDetails) {
//        Day tradePlan = repository.findByIdAndDeletedAtIsNull(id)
//                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        Day tradePlanEntity = mapper.toEntity(tradeDetails);
        Day savedDay = repository.save(tradePlanEntity);
        return mapper.toResponse(savedDay);
    }

    public DayResponse undoDelete(Long id) {
        Day tradePlan = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (tradePlan.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        // Undo the delete
        tradePlan.setDeletedAt(null);

        // Save and map to response
        Day savedDay = repository.save(tradePlan);
        return mapper.toResponse(savedDay);
    }

    public void delete(Long id) {
        Day tradePlan = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        tradePlan.setDeletedAt(LocalDateTime.now());
        repository.save(tradePlan);
    }
}