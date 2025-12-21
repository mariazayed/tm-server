package trademind.tm_server.tradeEvaluate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeEvaluate.TradeEvaluate;

import java.util.List;
import java.util.Optional;

// For basic CRUD operations

@Repository
public interface TradeEvaluateRepository extends JpaRepository<TradeEvaluate, Long> {
    List<TradeEvaluate> findByDeletedAtIsNull();

    Optional<TradeEvaluate> findByIdAndDeletedAtIsNull(Long id);
}