package trademind.tm_server.tradeMonitor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeMonitor.TradeMonitor;

// For dynamic search queries

@Repository
public interface TradeMonitorSearchRepository extends JpaRepository<TradeMonitor, Long>, JpaSpecificationExecutor<TradeMonitor> {
}
