package trademind.tm_server.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.trade.Trade;

// For dynamic search queries

@Repository
public interface TradeSearchRepository extends JpaRepository<Trade, Long>, JpaSpecificationExecutor<Trade> {
}
