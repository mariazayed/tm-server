package trademind.tm_server.memoir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.memoir.dto.MemoirResponse;
import trademind.tm_server.memoir.dto.NewMemoirRequest;
import trademind.tm_server.memoir.dto.UpdateMemoirRequest;
import trademind.tm_server.memoir.repository.MemoirRepository;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MemoirService {

    @Autowired
    private MemoirRepository repository;

    @Autowired
    private MemoirMapper mapper;

    private final String tableName = "Memoir";

    public MemoirResponse create(NewMemoirRequest trade) {
        Memoir entity = mapper.toEntity(trade);
        Memoir savedObj = repository.save(entity);
        return mapper.toResponse(savedObj);
    }

    public List<MemoirResponse> getAll() {
        List<Memoir> list = repository.findByDeletedAtIsNull();

        return list.stream()
                .map(mapper::toResponse)
                .toList();
    }

    public MemoirResponse getOneById(Long id) {
        Memoir obj = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        return mapper.toResponse(obj);
    }

    public Memoir getOneByIdAsEntity(Long id) {
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public MemoirResponse update(Long id, UpdateMemoirRequest tradeDetails) {
//        Day tradePlan = repository.findByIdAndDeletedAtIsNull(id)
//                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        Memoir entity = mapper.toEntity(tradeDetails);
        Memoir saved = repository.save(entity);
        return mapper.toResponse(saved);
    }

    public MemoirResponse undoDelete(Long id) {
        Memoir obj = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));

        if (obj.getDeletedAt() == null) {
            throw new BadRequestException(tableName + " with id " + id + " is not deleted");
        }

        // Undo the delete
        obj.setDeletedAt(null);

        // Save and map to response
        Memoir saved = repository.save(obj);
        return mapper.toResponse(saved);
    }

    public void delete(Long id) {
        Memoir obj = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
        obj.setDeletedAt(LocalDateTime.now());
        repository.save(obj);
    }
}