package trademind.tm_server.decision.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import trademind.tm_server.decision.Decision;

public interface DecisionSearchRepository extends JpaRepository<Decision, Long>, JpaSpecificationExecutor<Decision> {
}
