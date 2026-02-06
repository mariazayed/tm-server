package trademind.tm_server.strategy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.strategy.Strategy;

// For dynamic search queries

@Repository
public interface StrategySearchRepository extends JpaRepository<Strategy, Long>, JpaSpecificationExecutor<Strategy> {
}
