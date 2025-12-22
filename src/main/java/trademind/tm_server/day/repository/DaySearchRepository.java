package trademind.tm_server.day.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.day.Day;

// For dynamic search queries

@Repository
public interface DaySearchRepository extends JpaRepository<Day, Long>, JpaSpecificationExecutor<Day> {
}
