package trademind.tm_server.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.planner.PlannerWeek;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlannerWeekRepository extends JpaRepository<PlannerWeek, Long> {
    Optional<PlannerWeek> findByWeekStart(LocalDate weekStart);

    List<PlannerWeek> findByWeekStartBetweenAndDeletedAtIsNull(LocalDate from, LocalDate to);
}
