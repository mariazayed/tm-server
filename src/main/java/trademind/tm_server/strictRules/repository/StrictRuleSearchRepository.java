package trademind.tm_server.strictRules.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.strictRules.StrictRule;

// For dynamic search queries

@Repository
public interface StrictRuleSearchRepository extends JpaRepository<StrictRule, Long>, JpaSpecificationExecutor<StrictRule> {
}
