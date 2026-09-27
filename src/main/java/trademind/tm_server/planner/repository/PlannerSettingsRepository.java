package trademind.tm_server.planner.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.planner.PlannerSettings;

import java.util.Optional;

@Repository
public interface PlannerSettingsRepository extends JpaRepository<PlannerSettings, Long> {
    Optional<PlannerSettings> findFirstByDeletedAtIsNullOrderByIdAsc();
}
