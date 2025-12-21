package trademind.tm_server.tradeTerminate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeTerminate.TradeTerminate;

// For dynamic search queries

@Repository
public interface TradeTerminateSearchRepository extends JpaRepository<TradeTerminate, Long>, JpaSpecificationExecutor<TradeTerminate> {
}
