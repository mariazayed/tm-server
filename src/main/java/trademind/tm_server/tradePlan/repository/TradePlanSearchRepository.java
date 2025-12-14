package trademind.tm_server.tradePlan.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradePlan.TradePlan;

// For dynamic search queries

@Repository
public interface TradePlanSearchRepository extends JpaRepository<TradePlan, Long>, JpaSpecificationExecutor<TradePlan> {
}
