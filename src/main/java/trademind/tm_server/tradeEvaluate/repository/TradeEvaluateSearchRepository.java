package trademind.tm_server.tradeEvaluate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeEvaluate.TradeEvaluate;

// For dynamic search queries

@Repository
public interface TradeEvaluateSearchRepository extends JpaRepository<TradeEvaluate, Long>, JpaSpecificationExecutor<TradeEvaluate> {
}
