package trademind.tm_server.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.planner.PlannerWeekExtra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlannerWeekExtraRepository extends JpaRepository<PlannerWeekExtra, Long> {
    List<PlannerWeekExtra> findByWeekStartAndDeletedAtIsNullOrderByIdAsc(LocalDate weekStart);

    List<PlannerWeekExtra> findByWeekStartBetweenAndDeletedAtIsNull(LocalDate from, LocalDate to);

    Optional<PlannerWeekExtra> findByIdAndDeletedAtIsNull(Long id);
}
