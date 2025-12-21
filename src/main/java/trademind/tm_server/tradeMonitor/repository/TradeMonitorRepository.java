package trademind.tm_server.tradeMonitor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeMonitor.TradeMonitor;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface TradeMonitorRepository extends JpaRepository<TradeMonitor, Long> {
    List<TradeMonitor> findByDeletedAtIsNull();

    Optional<TradeMonitor> findByIdAndDeletedAtIsNull(Long id);
}