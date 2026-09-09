package trademind.tm_server.tradeStep.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import trademind.tm_server.tradeStep.TradeStep;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeStepRepository extends JpaRepository<TradeStep, Long> {
    List<TradeStep> findByDeletedAtIsNull();

    Optional<TradeStep> findByIdAndDeletedAtIsNull(Long id);

    List<TradeStep> findByTradeIdAndDeletedAtIsNull(Long tradeId);
}
