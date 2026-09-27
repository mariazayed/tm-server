package trademind.tm_server.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.planner.FocusArea;

import java.util.List;
import java.util.Optional;

@Repository
public interface FocusAreaRepository extends JpaRepository<FocusArea, Long> {
    List<FocusArea> findByDeletedAtIsNullOrderBySortOrderAscIdAsc();

    Optional<FocusArea> findByIdAndDeletedAtIsNull(Long id);
}
