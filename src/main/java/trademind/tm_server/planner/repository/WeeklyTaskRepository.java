package trademind.tm_server.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.planner.WeeklyTask;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyTaskRepository extends JpaRepository<WeeklyTask, Long> {
    List<WeeklyTask> findByWeekStartAndDeletedAtIsNullOrderBySortOrderAscIdAsc(LocalDate weekStart);

    List<WeeklyTask> findByWeekStartBetweenAndDeletedAtIsNull(LocalDate from, LocalDate to);

    List<WeeklyTask> findByMonthlyObjectiveIdInAndDeletedAtIsNull(Collection<Long> objectiveIds);

    List<WeeklyTask> findByMonthlyObjectiveIdAndDeletedAtIsNull(Long objectiveId);

    Optional<WeeklyTask> findByIdAndDeletedAtIsNull(Long id);
}
