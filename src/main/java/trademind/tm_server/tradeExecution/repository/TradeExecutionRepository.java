package trademind.tm_server.tradeExecution.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeExecution.TradeExecution;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface TradeExecutionRepository extends JpaRepository<TradeExecution, Long> {
    List<TradeExecution> findByDeletedAtIsNull();

    Optional<TradeExecution> findByIdAndDeletedAtIsNull(Long id);
}