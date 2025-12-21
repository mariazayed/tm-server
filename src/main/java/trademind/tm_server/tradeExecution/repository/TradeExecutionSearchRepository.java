package trademind.tm_server.tradeExecution.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeExecution.TradeExecution;

// For dynamic search queries

@Repository
public interface TradeExecutionSearchRepository extends JpaRepository<TradeExecution, Long>, JpaSpecificationExecutor<TradeExecution> {
}
