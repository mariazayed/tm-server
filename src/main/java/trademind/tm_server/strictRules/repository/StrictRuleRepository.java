package trademind.tm_server.strictRules.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.strictRules.StrictRule;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface StrictRuleRepository extends JpaRepository<StrictRule, Long> {
    List<StrictRule> findByDeletedAtIsNull();

    Optional<StrictRule> findByIdAndDeletedAtIsNull(Long id);
}