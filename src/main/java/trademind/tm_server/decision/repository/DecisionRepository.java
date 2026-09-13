package trademind.tm_server.decision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import trademind.tm_server.decision.Decision;

import java.util.List;
import java.util.Optional;

public interface DecisionRepository extends JpaRepository<Decision, Long> {
    List<Decision> findByDeletedAtIsNull();

    Optional<Decision> findByIdAndDeletedAtIsNull(Long id);

    // Used to auto-assign the next decisionNumber on create (see
    // DecisionService.create). Looks across all rows, including
    // soft-deleted ones, so a number is never reused.
    @Query("SELECT COALESCE(MAX(d.decisionNumber), 0) FROM Decision d")
    Integer findMaxDecisionNumber();
}
