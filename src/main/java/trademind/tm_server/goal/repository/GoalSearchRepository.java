package trademind.tm_server.goal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.goal.Goal;

// For dynamic search queries

@Repository
public interface GoalSearchRepository extends JpaRepository<Goal, Long>, JpaSpecificationExecutor<Goal> {
}
