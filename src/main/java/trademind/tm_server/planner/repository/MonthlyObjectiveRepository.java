package trademind.tm_server.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.planner.MonthlyObjective;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyObjectiveRepository extends JpaRepository<MonthlyObjective, Long> {
    List<MonthlyObjective> findByPlanYearAndPlanMonthAndDeletedAtIsNullOrderBySortOrderAscIdAsc(Integer planYear, Integer planMonth);

    Optional<MonthlyObjective> findByIdAndDeletedAtIsNull(Long id);
}
