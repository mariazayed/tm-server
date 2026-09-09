package trademind.tm_server.goal.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.goal.Goal;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface GoalRepository extends JpaRepository<Goal, Long> {
    List<Goal> findByDeletedAtIsNull();

    Optional<Goal> findByIdAndDeletedAtIsNull(Long id);
}
