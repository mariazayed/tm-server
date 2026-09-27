package trademind.tm_server.planner;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.ResourceNotFoundException;
import trademind.tm_server.planner.dto.FocusAreaRequest;
import trademind.tm_server.planner.dto.FocusAreaResponse;
import trademind.tm_server.planner.repository.FocusAreaRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FocusAreaService {

    @Autowired
    private FocusAreaRepository repository;

    @Autowired
    private PlannerMapper mapper;

    private final String tableName = "FocusArea";

    public List<FocusAreaResponse> getAll() {
        return repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
                .map(mapper::toResponse)
                .toList();
    }

    public FocusArea getOneByIdAsEntity(Long id) {
        if (id == null) throw new BadRequestException("focusAreaId is required");
        return repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException(tableName, "id", id));
    }

    public FocusAreaResponse create(FocusAreaRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BadRequestException("Focus area name is required");
        }
        if (request.getMaxPercent() == null) {
            throw new BadRequestException("Focus area max percent is required");
        }

        FocusArea area = new FocusArea();
        area.setName(request.getName().trim());
        area.setColor(request.getColor() != null ? request.getColor() : "#6366F1");
        area.setIcon(request.getIcon());
        area.setMaxPercent(validPercent(request.getMaxPercent()));
        area.setSortOrder(request.getSortOrder() != null
                ? request.getSortOrder()
                : repository.findByDeletedAtIsNullOrderBySortOrderAscIdAsc().size());
        area.setArchived(Boolean.TRUE.equals(request.getArchived()));
        area.setNotes(request.getNotes());

        return mapper.toResponse(repository.save(area));
    }

    public FocusAreaResponse update(Long id, FocusAreaRequest request) {
        FocusArea area = getOneByIdAsEntity(id);

        if (request.getName() != null && !request.getName().isBlank()) area.setName(request.getName().trim());
        if (request.getColor() != null) area.setColor(request.getColor());
        if (request.getIcon() != null) area.setIcon(request.getIcon());
        if (request.getMaxPercent() != null) area.setMaxPercent(validPercent(request.getMaxPercent()));
        if (request.getSortOrder() != null) area.setSortOrder(request.getSortOrder());
        if (request.getArchived() != null) area.setArchived(request.getArchived());
        if (request.getNotes() != null) area.setNotes(request.getNotes());

        return mapper.toResponse(repository.save(area));
    }

    // Soft delete: tasks/objectives already using the area keep showing it.
    public void delete(Long id) {
        FocusArea area = getOneByIdAsEntity(id);
        area.setDeletedAt(LocalDateTime.now());
        repository.save(area);
    }

    private double validPercent(Double value) {
        if (value < 0 || value > 100) {
            throw new BadRequestException("Max percent must be between 0 and 100");
        }
        return value;
    }
}
